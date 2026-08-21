package com.ingot.cloud.security.service.session.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.user.InnerUserDTO;
import com.ingot.cloud.pms.api.rpc.RemotePmsTenantDetailsService;
import com.ingot.cloud.pms.api.rpc.RemotePmsUserDetailsService;
import com.ingot.cloud.security.api.model.dto.session.PlatformSessionQueryDTO;
import com.ingot.cloud.security.api.model.dto.session.PlatformUserSessionRevokeDTO;
import com.ingot.cloud.security.api.model.vo.session.PlatformSessionVO;
import com.ingot.cloud.security.model.convert.PlatformSessionConvert;
import com.ingot.cloud.security.service.session.SessionAdminService;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>{@link SessionAdminService} 默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote 指定 clientId 时直接复用 Auth Inner 的分页（游标建立在「租户 + Client」的在线用户有序集上）；
 * 只给 userId 时改走 Inner 的按用户查询以支持跨 Client，结果集有界，由本层排序后内存分页。
 * 名称补全在分页切片之后进行，避免为不展示的记录调用 PMS。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAdminServiceImpl implements SessionAdminService {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 20L;

    /**
     * 每页条数上限，与 Auth Inner 保持一致，避免管理面一次性拉走全部会话。
     */
    private static final long MAX_SIZE = 200L;

    private static final String MSG_TENANT_REQUIRED = "SessionAdminService.TenantRequired";
    private static final String MSG_QUERY_SCOPE_REQUIRED = "SessionAdminService.QueryScopeRequired";
    private static final String MSG_USER_REQUIRED = "SessionAdminService.UserRequired";

    /**
     * 会话创建时间倒序，创建时间缺失的排在最后。
     */
    private static final Comparator<PlatformSessionVO> LATEST_FIRST = Comparator.comparing(
            PlatformSessionVO::getIssuedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final RemoteAuthSessionService remoteAuthSessionService;
    private final RemotePmsUserDetailsService remotePmsUserDetailsService;
    private final RemotePmsTenantDetailsService remotePmsTenantDetailsService;
    private final PlatformSessionConvert sessionConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public IPage<PlatformSessionVO> page(PlatformSessionQueryDTO params) {
        Long tenantId = resolveTenantId(params.getTenantId());
        assertionChecker.checkOperation(
                StrUtil.isNotEmpty(params.getClientId()) || params.getUserId() != null,
                MSG_QUERY_SCOPE_REQUIRED, "查询在线会话必须指定 clientId 或 userId");

        long current = pageNumber(params.getCurrent());
        long size = pageSize(params.getSize());
        return StrUtil.isNotEmpty(params.getClientId())
                ? clientScopedPage(tenantId, params, current, size)
                : userScopedPage(tenantId, params, current, size);
    }

    @Override
    public PlatformSessionVO getBySid(String sid) {
        InnerSessionVO session = remoteAuthSessionService.getBySid(sid)
                .ifErrorThrow()
                .getData();
        if (session == null) {
            return null;
        }

        List<PlatformSessionVO> records = new ArrayList<>();
        records.add(sessionConvert.to(session));
        enrich(session.getTenantId(), records);
        return records.get(0);
    }

    @Override
    public boolean revokeBySid(String sid, Long actorId) {
        InnerSessionRevokeDTO revoke = InnerSessionRevokeDTO.builder()
                .reason(SessionRevokeReason.ADMIN_REVOKE)
                .actorId(actorId)
                .build();
        // 不透传管理员自己的 Cookie：目标会话的 Auth 登录态无法从管理面清理，
        // 误传只会把操作者自己的授权服务器登录态一起清掉
        Boolean revoked = remoteAuthSessionService.revokeBySid(null, sid, revoke)
                .ifErrorThrow()
                .getData();
        log.info("[SessionAdmin] 管理员下线会话: sid={}, actorId={}, revoked={}", sid, actorId, revoked);
        return BooleanUtil.isTrue(revoked);
    }

    @Override
    public int revokeByUser(PlatformUserSessionRevokeDTO params, Long actorId) {
        Long tenantId = resolveTenantId(params.getTenantId());
        assertionChecker.checkOperation(params.getUserId() != null,
                MSG_USER_REQUIRED, "按用户下线必须指定 userId");

        InnerUserSessionRevokeDTO revoke = InnerUserSessionRevokeDTO.builder()
                .tenantId(tenantId)
                .userId(params.getUserId())
                .clientId(params.getClientId())
                .reason(SessionRevokeReason.ADMIN_REVOKE)
                .actorId(actorId)
                .build();
        Integer revoked = remoteAuthSessionService.revokeByUser(revoke)
                .ifErrorThrow()
                .getData();
        log.info("[SessionAdmin] 管理员按用户下线会话: tenantId={}, userId={}, clientId={}, actorId={}, count={}",
                tenantId, params.getUserId(), params.getClientId(), actorId, revoked);
        return revoked == null ? 0 : revoked;
    }

    /**
     * 指定 Client 的分页，直接沿用 Auth 的分页游标。
     *
     * <p>无 userId / ipAddress 过滤时 {@code total} 是在线用户数而非会话数，
     * 多会话模式下同一页可能返回多于 {@code size} 条记录，该语义与 Auth Inner 一致。</p>
     */
    private IPage<PlatformSessionVO> clientScopedPage(Long tenantId, PlatformSessionQueryDTO params,
                                                      long current, long size) {
        InnerSessionQueryDTO query = InnerSessionQueryDTO.builder()
                .tenantId(tenantId)
                .clientId(params.getClientId())
                .userId(params.getUserId())
                .ipAddress(params.getIpAddress())
                .current(current)
                .size(size)
                .build();
        InnerSessionPageVO result = remoteAuthSessionService.page(query)
                .ifErrorThrow()
                .getData();
        if (result == null) {
            return emptyPage(current, size);
        }
        return toPage(tenantId, current, size, result.getTotal(), result.getRecords());
    }

    /**
     * 只给 userId 的分页：跨 Client 查询该用户会话后内存分页，总数为精确会话数。
     */
    private IPage<PlatformSessionVO> userScopedPage(Long tenantId, PlatformSessionQueryDTO params,
                                                    long current, long size) {
        InnerSessionQueryDTO query = InnerSessionQueryDTO.builder()
                .tenantId(tenantId)
                .userId(params.getUserId())
                .build();
        List<InnerSessionVO> sessions = remoteAuthSessionService.listByUser(query)
                .ifErrorThrow()
                .getData();
        if (sessions == null || sessions.isEmpty()) {
            return emptyPage(current, size);
        }

        List<PlatformSessionVO> all = new ArrayList<>(sessionConvert.to(sessions));
        all.sort(LATEST_FIRST);
        int total = all.size();
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<PlatformSessionVO> records = new ArrayList<>(all.subList(from, to));

        Page<PlatformSessionVO> page = new Page<>(current, size, total);
        page.setRecords(enrich(tenantId, records));
        return page;
    }

    private IPage<PlatformSessionVO> toPage(Long tenantId, long current, long size,
                                            long total, List<InnerSessionVO> records) {
        Page<PlatformSessionVO> page = new Page<>(current, size, total);
        List<PlatformSessionVO> converted = records == null
                ? new ArrayList<>() : new ArrayList<>(sessionConvert.to(records));
        page.setRecords(enrich(tenantId, converted));
        return page;
    }

    private IPage<PlatformSessionVO> emptyPage(long current, long size) {
        return new Page<>(current, size, 0L);
    }

    /**
     * 用 PMS 数据补全展示名称。
     *
     * <p>PMS 是管理面的旁路依赖：查询失败只让名称为空，不影响 sid 级字段与下线操作。
     * C 端用户不在 PMS 用户表内，只保留会话里的登录账号名。</p>
     */
    private List<PlatformSessionVO> enrich(Long tenantId, List<PlatformSessionVO> records) {
        if (records.isEmpty()) {
            return records;
        }

        String tenantName = queryTenantName(tenantId);
        Map<Long, InnerUserDTO> users = queryAdminUsers(records);
        for (PlatformSessionVO record : records) {
            record.setTenantName(tenantName);
            InnerUserDTO user = users.get(record.getUserId());
            if (user == null) {
                continue;
            }
            record.setNickname(user.getNickname());
            record.setAvatar(user.getAvatar());
            if (StrUtil.isNotEmpty(user.getUsername())) {
                record.setUsername(user.getUsername());
            }
        }
        return records;
    }

    private String queryTenantName(Long tenantId) {
        try {
            R<SysTenant> response = remotePmsTenantDetailsService.getTenantById(tenantId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().getName();
            }
            log.warn("[SessionAdmin] 查询租户名称失败, tenantId={}, response={}", tenantId, response);
        } catch (Exception e) {
            log.warn("[SessionAdmin] 查询租户名称异常, tenantId={}", tenantId, e);
        }
        return null;
    }

    private Map<Long, InnerUserDTO> queryAdminUsers(List<PlatformSessionVO> records) {
        Set<Long> userIds = records.stream()
                .filter(record -> StrUtil.equals(record.getUserType(), UserTypeEnum.ADMIN.getValue()))
                .map(PlatformSessionVO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }

        try {
            R<List<InnerUserDTO>> response = remotePmsUserDetailsService.getAllUserInfo(new ArrayList<>(userIds));
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().stream()
                        .filter(user -> user.getId() != null)
                        .collect(Collectors.toMap(InnerUserDTO::getId, Function.identity(), (a, b) -> a));
            }
            log.warn("[SessionAdmin] 查询用户名称失败, userIds={}, response={}", userIds, response);
        } catch (Exception e) {
            log.warn("[SessionAdmin] 查询用户名称异常, userIds={}", userIds, e);
        }
        return Map.of();
    }

    /**
     * 租户维度是会话键空间的一部分，缺失时无法定位会话，不能默认「全部租户」。
     */
    private Long resolveTenantId(Long tenantId) {
        Long resolved = tenantId != null ? tenantId : TenantContextHolder.get();
        assertionChecker.checkOperation(resolved != null,
                MSG_TENANT_REQUIRED, "查询在线会话必须指定 tenantId");
        return resolved;
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
