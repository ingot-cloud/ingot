package com.ingot.cloud.gateway.security;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步向 ingot-service-security 上报封禁审计事件。
 *
 * <p>使用单线程池 + 队列；失败仅打日志，不阻塞主链路。</p>
 *
 * <p><b>循环依赖说明</b>：必须保持 {@link RemoteSecurityPolicyService} 通过 {@link ObjectProvider}
 * 懒解析（即 {@link #report(BlacklistReportDTO)} 调用时才 {@code getIfAvailable}），否则会触发
 * Feign client → Spring Cloud Gateway {@code filteringWebHandler} → SentinelSCGAutoConfiguration
 * → {@link SentinelBlockHandler} → 当前类的循环。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
public class BlacklistEventReporter {

    private final ObjectProvider<RemoteSecurityPolicyService> remoteProvider;
    private final ExecutorService executor;

    public BlacklistEventReporter(ObjectProvider<RemoteSecurityPolicyService> remoteProvider) {
        this.remoteProvider = remoteProvider;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "in-gw-blacklist-reporter");
            t.setDaemon(true);
            return t;
        });
    }

    public void report(BlacklistReportDTO dto) {
        if (dto == null) return;
        executor.execute(() -> {
            RemoteSecurityPolicyService remoteService = remoteProvider.getIfAvailable();
            if (remoteService == null) {
                log.debug("[BlacklistEventReporter] RemoteSecurityPolicyService not available, skip");
                return;
            }
            try {
                remoteService.reportBlacklist(dto);
            } catch (Exception e) {
                log.warn("[BlacklistEventReporter] report failed: {}", e.getMessage());
            }
        });
    }
}
