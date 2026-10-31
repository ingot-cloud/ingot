package com.ingot.cloud.bff.service;

import java.util.UUID;
import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.BffCleanup;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 可靠回收已知未交付/被替换的Auth sid；失败保留载荷并有界重试。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BffCleanupService {

    private final LoginTransactionService store;

    private final RemoteAuthSessionService auth;

    private final BffProperties properties;

    private final BffMetrics metrics;

    /** 按应用读取有界到期任务，租约失效只能幂等撤销，不能签发身份。 */
    @Scheduled(fixedDelayString = BffConstants.CLEANUP_INTERVAL_EXPRESSION)
    public void cleanup() {
        for (var app : properties.getApps()) {
            try {
                cleanupApp(app.getAppId());
            }
            catch (RuntimeException exception) {
                log.warn("[BffCleanup] cleanup unavailable, app={}", app.getAppId());
            }
        }
    }

    /** 尚未尝试正式提交、Redis无法记账时，尽力直接撤销本工作者已知sid。 */
    public void revokeUncommitted(com.ingot.framework.commons.model.bff.BffSession session) {
        try {
            if (session.getSid() == null)
                return;
            R<Boolean> result = auth.revokeBySid(session.getAuthCookie(), session.getSid(),
                    InnerSessionRevokeDTO.builder().reason(SessionRevokeReason.USER_LOGOUT).build());
            if (result == null || !result.isSuccess())
                throw new IllegalStateException("Auth revoke failed");
            metrics.event("cleanup", session.getAppId(), "direct");
        }
        catch (RuntimeException exception) {
            metrics.event("cleanup", session.getAppId(), "unrecorded");
            log.error(
                    "[BffCleanup] Redis/Auth unavailable before commit; Auth TTL/manual reconciliation required, app={}",
                    session.getAppId());
        }
    }

    /** 可在退出后立即执行本应用清理；失败仍留在持久队列。 */
    public void cleanupApp(String appId) {
        metrics.backlog(appId, store.cleanupSize(appId));
        for (String id : store.due(appId)) {
            String owner = UUID.randomUUID().toString();
            BffCleanup job = store.claimCleanup(appId, id, owner);
            if (job == null)
                continue;
            try {
                if (job.getSid() != null && !job.getSid().isBlank()) {
                    R<Boolean> result = auth.revokeBySid(job.getAuthCookie(), job.getSid(),
                            InnerSessionRevokeDTO.builder().reason(SessionRevokeReason.USER_LOGOUT).build());
                    if (result == null || !result.isSuccess())
                        throw new IllegalStateException("Auth revoke failed");
                }
                store.finishCleanup(job, owner);
                metrics.event("cleanup", appId, "completed");
            }
            catch (RuntimeException exception) {
                metrics.event("cleanup", appId, "pending");
                log.warn("[BffCleanup] revoke pending; retained for retry, app={}", appId);
            }
        }
    }

}
