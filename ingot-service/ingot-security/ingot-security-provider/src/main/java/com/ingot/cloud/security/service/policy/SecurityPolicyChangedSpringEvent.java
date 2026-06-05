package com.ingot.cloud.security.service.policy;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 安全策略写操作变更（Spring 内部事件，事务提交后由
 * {@link SecurityPolicyInvalidationPublisher} 监听并触发跨节点广播）。
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
public class SecurityPolicyChangedSpringEvent extends ApplicationEvent {

    private final SecurityPolicyDomain domain;

    public SecurityPolicyChangedSpringEvent(Object source, SecurityPolicyDomain domain) {
        super(source);
        this.domain = domain;
    }
}
