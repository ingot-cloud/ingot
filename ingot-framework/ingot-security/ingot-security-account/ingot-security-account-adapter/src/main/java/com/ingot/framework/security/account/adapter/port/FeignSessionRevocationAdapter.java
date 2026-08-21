package com.ingot.framework.security.account.adapter.port;

import java.util.concurrent.atomic.LongAdder;

import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.SessionRevocationPort;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>经 Auth Inner RPC 撤销会话的 {@link SessionRevocationPort} 实现。</p>
 *
 * <p>会话主数据只在 Auth 侧 Redis，账号域不直接操作存储，统一由
 * {@link RemoteAuthSessionService} 完成撤销并由 Auth 记录安全事件。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote fail-open：RPC 异常、租户上下文缺失、Auth 返回失败都只记日志与失败计数，
 * 不向账号域抛出，避免锁定/改密本身被联动失败拖累。失败计数由
 * {@code ingot.security.session.revoke.linkage.failure} 暴露，需配置告警。
 */
@Slf4j
@RequiredArgsConstructor
public class FeignSessionRevocationAdapter implements SessionRevocationPort {

    private final RemoteAuthSessionService remoteAuthSessionService;

    private final LongAdder revokedSessions = new LongAdder();
    private final LongAdder failures = new LongAdder();

    @Override
    public int revokeUserSessions(Long userId, SessionRevokeReason reason, Long actorId) {
        if (userId == null) {
            return 0;
        }

        Long tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            // 无租户上下文时无法定位会话键空间，继续调用只会撤销失败
            failures.increment();
            log.warn("[SessionRevoke] 缺少租户上下文，跳过会话撤销: userId={}, reason={}", userId, reason);
            return 0;
        }

        InnerUserSessionRevokeDTO params = InnerUserSessionRevokeDTO.builder()
                .tenantId(tenantId)
                .userId(userId)
                .reason(reason)
                .actorId(actorId)
                .build();

        try {
            R<Integer> response = remoteAuthSessionService.revokeByUser(params);
            if (!response.isSuccess()) {
                failures.increment();
                log.error("[SessionRevoke] Auth 返回失败: userId={}, reason={}, code={}, message={}",
                        userId, reason, response.getCode(), response.getMessage());
                return 0;
            }

            int revoked = response.getData() == null ? 0 : response.getData();
            revokedSessions.add(revoked);
            log.info("[SessionRevoke] 已撤销会话: tenantId={}, userId={}, reason={}, count={}",
                    tenantId, userId, reason, revoked);
            return revoked;
        } catch (Exception e) {
            failures.increment();
            log.error("[SessionRevoke] 调用 Auth 撤销会话异常: userId={}, reason={}", userId, reason, e);
            return 0;
        }
    }

    /**
     * 累计撤销成功的会话数。
     */
    public long getRevokedSessions() {
        return revokedSessions.sum();
    }

    /**
     * 累计撤销失败次数，非零意味着存在「账号已锁但会话仍在线」的窗口。
     */
    public long getFailures() {
        return failures.sum();
    }
}
