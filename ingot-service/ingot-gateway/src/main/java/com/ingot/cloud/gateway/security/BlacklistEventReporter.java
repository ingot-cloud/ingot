package com.ingot.cloud.gateway.security;

import com.ingot.cloud.gateway.config.BlacklistEventReporterConfiguration;
import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventCategory;
import com.ingot.cloud.security.api.support.BlacklistReportEventMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
import lombok.RequiredArgsConstructor;

/**
 * 异步向 ingot-security 上报访问防护类安全事件。
 *
 * <p>触发场景：{@link SentinelBlockHandler} 在限流违规达阈值后写入
 * {@link TempBlockStore} 时，附带 keyType / keyValue / ruleCode 等信息调用 {@link #report}。
 * 静态名单命中由 {@link BlacklistFilter} 本地拒绝，通常不上报。</p>
 *
 * <p>经统一 {@link SecurityEventReportPublisher} 发布；队列满时丢弃，不阻塞主链路。
 * Bean 由 {@link BlacklistEventReporterConfiguration}
 * 在 ReportPublisher 就绪后注册，避免 {@code @Component} 上 {@code @ConditionalOnBean} 评估过早。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@RequiredArgsConstructor
public class BlacklistEventReporter {

    private final SecurityEventReportPublisher reportPublisher;
    private final SecurityEventProperties recordingProperties;

    public void report(BlacklistReportDTO dto) {
        if (dto == null || !recordingProperties.isEnabled()) {
            return;
        }
        if (!recordingProperties.isCategoryEnabled(SecurityEventCategory.ACCESS.name())) {
            return;
        }
        SecurityEventReportDTO event = BlacklistReportEventMapper.toSecurityEvent(dto);
        if (event == null) {
            return;
        }
        reportPublisher.publish(event);
    }
}
