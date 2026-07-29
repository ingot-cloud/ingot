package com.ingot.cloud.gateway.security;

import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventCategory;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.cloud.security.api.support.AsyncSecurityEventReporter;
import com.ingot.cloud.security.api.support.BlacklistReportEventMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 异步向 ingot-security 上报访问防护类安全事件。
 *
 * <p>触发场景：{@link SentinelBlockHandler} 在限流违规达阈值后写入
 * {@link TempBlockStore} 时，附带 keyType / keyValue / ruleCode 等信息调用 {@link #report}。
 * 静态名单命中由 {@link BlacklistFilter} 本地拒绝，通常不上报。</p>
 *
 * <p>使用有界队列 + 攒批上报；队列满时丢弃并打 warn，不阻塞主链路。</p>
 *
 * <p><b>循环依赖说明</b>：{@link RemoteSecurityEventService} 必须通过 {@link ObjectProvider}
 * 懒解析，避免 Feign 与 Sentinel 过滤链形成 Bean 循环。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Component
@RequiredArgsConstructor
public class BlacklistEventReporter {

    private final ObjectProvider<RemoteSecurityEventService> remoteEventProvider;
    private final SecurityEventProperties properties;
    private AsyncSecurityEventReporter reporter;

    @PostConstruct
    void init() {
        reporter = new AsyncSecurityEventReporter(
                remoteEventProvider::getIfAvailable,
                properties,
                "in-gw-security-event-reporter",
                "BlacklistEventReporter");
    }

    @PreDestroy
    void destroy() {
        if (reporter != null) {
            reporter.close();
        }
    }

    public void report(BlacklistReportDTO dto) {
        if (dto == null || !properties.isRemoteCategoryEnabled(SecurityEventCategory.ACCESS.name())) {
            return;
        }
        SecurityEventReportDTO event = BlacklistReportEventMapper.toSecurityEvent(dto);
        if (event == null) {
            return;
        }
        reporter.offer(event);
    }
}
