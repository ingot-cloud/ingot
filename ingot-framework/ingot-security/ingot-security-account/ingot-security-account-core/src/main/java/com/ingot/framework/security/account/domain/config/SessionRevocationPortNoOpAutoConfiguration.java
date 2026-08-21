package com.ingot.framework.security.account.domain.config;

import com.ingot.framework.security.account.domain.port.outbound.SessionRevocationPort;
import com.ingot.framework.security.account.domain.port.outbound.noop.NoOpSessionRevocationPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>adapter 未提供 {@link SessionRevocationPort} 时的 NoOp 回退。</p>
 *
 * <p>须在 adapter 的装配之后评估，避免过早占位导致 Feign 实现无法注册；
 * 联动开关关闭时也会落到这里，账号状态变更本身不受影响。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(name =
        "com.ingot.framework.security.account.adapter.config.SessionRevocationPortAutoConfiguration")
@ConditionalOnMissingBean(SessionRevocationPort.class)
public class SessionRevocationPortNoOpAutoConfiguration {

    @Bean
    public SessionRevocationPort noOpSessionRevocationPort() {
        return new NoOpSessionRevocationPort();
    }
}
