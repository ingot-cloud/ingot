package com.ingot.cloud.auth.service.biz.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.model.convert.SessionConvert;
import com.ingot.cloud.auth.service.biz.BizSessionService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>{@link BizSessionService} 默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote 无过滤条件的分页以「在线用户有序集」为游标再展开会话，只读取当前页用户的会话，
 * 避免把全租户会话载入内存；带 userId 或 IP 过滤时结果集本身有界，直接内存分页以获得精确总数。
 */
@Service
@RequiredArgsConstructor
public class BizSessionServiceImpl implements BizSessionService {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 20L;

    /**
     * 每页条数上限，避免调用方一次性拉走全部会话。
     */
    private static final long MAX_SIZE = 200L;

    private static final String MSG_PAGE_SCOPE_REQUIRED = "BizSessionService.PageScopeRequired";
    private static final String MSG_USER_SCOPE_REQUIRED = "BizSessionService.UserScopeRequired";

    /**
     * 会话创建时间倒序，创建时间缺失的排在最后。
     */
    private static final Comparator<OnlineToken> LATEST_FIRST = Comparator.comparing(
            OnlineToken::getIssuedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final OnlineTokenService onlineTokenService;
    private final SessionConvert sessionConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public InnerSessionPageVO page(InnerSessionQueryDTO params) {
        assertionChecker.checkOperation(
                params.getTenantId() != null && StrUtil.isNotEmpty(params.getClientId()),
                MSG_PAGE_SCOPE_REQUIRED, "分页查询会话必须指定 tenantId 与 clientId");

        long current = pageNumber(params.getCurrent());
        long size = pageSize(params.getSize());
        if (params.getUserId() != null || StrUtil.isNotEmpty(params.getIpAddress())) {
            return sliceInMemory(filteredSessions(params), current, size);
        }
        return onlineUserPage(params, current, size);
    }

    @Override
    public InnerSessionVO getBySid(String sid) {
        return onlineTokenService.getBySid(sid).map(sessionConvert::to).orElse(null);
    }

    @Override
    public List<InnerSessionVO> listByUser(InnerSessionQueryDTO params) {
        assertionChecker.checkOperation(
                params.getTenantId() != null && params.getUserId() != null,
                MSG_USER_SCOPE_REQUIRED, "按用户查询会话必须指定 tenantId 与 userId");

        List<OnlineToken> sessions = StrUtil.isEmpty(params.getClientId())
                ? onlineTokenService.listUserSessions(params.getTenantId(), params.getUserId())
                : onlineTokenService.listUserSessions(
                params.getTenantId(), params.getClientId(), params.getUserId());
        return sessionConvert.to(sessions);
    }

    /**
     * 按在线用户分页并展开其会话。
     *
     * <p>{@code total} 是在线用户数而非会话数 —— 分页游标建立在在线用户有序集上，
     * 单会话模式下两者一致，多会话模式下同一页可能返回多于 {@code size} 条记录。</p>
     */
    private InnerSessionPageVO onlineUserPage(InnerSessionQueryDTO params, long current, long size) {
        Long tenantId = params.getTenantId();
        String clientId = params.getClientId();

        long total = onlineTokenService.getOnlineUserCount(tenantId, clientId);
        if (total == 0) {
            return InnerSessionPageVO.empty(current, size);
        }

        List<Long> userIds = onlineTokenService.getOnlineUsers(
                tenantId, clientId, (current - 1) * size, size);
        List<OnlineToken> sessions = onlineTokenService.listUserSessions(
                tenantId, clientId, userIds);
        return InnerSessionPageVO.builder()
                .current(current)
                .size(size)
                .total(total)
                .records(sessionConvert.to(sessions))
                .build();
    }

    /**
     * 按 userId 或 IP 命中的会话集合，已按创建时间倒序。
     */
    private List<InnerSessionVO> filteredSessions(InnerSessionQueryDTO params) {
        List<String> sids = StrUtil.isNotEmpty(params.getIpAddress())
                ? onlineTokenService.listSidsByIp(params.getTenantId(), params.getIpAddress())
                : onlineTokenService.listSids(
                params.getTenantId(), params.getClientId(), params.getUserId());

        List<OnlineToken> sessions = sids.stream()
                .map(onlineTokenService::getBySid)
                .flatMap(Optional::stream)
                .filter(session -> matches(session, params))
                .sorted(LATEST_FIRST)
                .toList();
        return sessionConvert.to(sessions);
    }

    /**
     * IP 索引跨 Client 且不区分用户，命中后仍需按查询条件二次过滤。
     */
    private boolean matches(OnlineToken session, InnerSessionQueryDTO params) {
        if (!StrUtil.equals(params.getClientId(), session.getClientId())) {
            return false;
        }
        return params.getUserId() == null || params.getUserId().equals(session.getUserId());
    }

    private InnerSessionPageVO sliceInMemory(List<InnerSessionVO> sessions, long current, long size) {
        int total = sessions.size();
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        return InnerSessionPageVO.builder()
                .current(current)
                .size(size)
                .total(total)
                .records(new ArrayList<>(sessions.subList(from, to)))
                .build();
    }

    private long pageNumber(Long current) {
        return current == null || current < DEFAULT_CURRENT ? DEFAULT_CURRENT : current;
    }

    private long pageSize(Long size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
