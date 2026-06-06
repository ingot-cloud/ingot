package com.ingot.cloud.gateway.security;

import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步向 ingot-security 上报封禁审计事件。
 *
 * <p>触发场景：{@link SentinelBlockHandler} 在限流违规达阈值后写入
 * {@link TempBlockStore} 时，附带 keyType / keyValue / ruleCode 等信息调用 {@link #report}。
 * 静态名单命中由 {@link BlacklistFilter} 本地拒绝，通常不上报。</p>
 *
 * <p>使用守护线程单线程池排队执行；RPC 失败仅打 warn 日志，不阻塞、不重试主链路。</p>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * # Feign 目标服务须可达；security 未注册时 getIfAvailable 为空，静默跳过
 * spring:
 *   cloud:
 *     openfeign:
 *       client:
 *         config:
 *           ingot-security:
 *             connectTimeout: 2000
 * }</pre>
 *
 * <p><b>循环依赖说明</b>：{@link RemoteSecurityPolicyService} 必须通过 {@link ObjectProvider}
 * 懒解析（{@link #report} 调用时才 {@code getIfAvailable}），否则会形成
 * Feign → {@code filteringWebHandler} → SentinelSCGAutoConfiguration
 * → {@link SentinelBlockHandler} → 本类的 Bean 循环。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistEventReporter {

    private final ObjectProvider<RemoteSecurityPolicyService> remoteProvider;
    private ExecutorService executor;

    @PostConstruct
    void init() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "in-gw-blacklist-reporter");
            t.setDaemon(true);
            return t;
        });
    }

    public void report(BlacklistReportDTO dto) {
        if (dto == null) {
            return;
        }
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
