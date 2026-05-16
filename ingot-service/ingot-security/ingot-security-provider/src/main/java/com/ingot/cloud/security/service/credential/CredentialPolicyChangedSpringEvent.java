package com.ingot.cloud.security.service.credential;

import org.springframework.context.ApplicationEvent;

/**
 * 凭证策略配置变更的本地 Spring 事件，由 ingot-security-provider 写操作发布；
 * 事务提交后由 {@link CredentialInvalidationPublisher} 转发为跨节点失效广播。
 *
 * @author jy
 * @since 2026/5/16
 */
public class CredentialPolicyChangedSpringEvent extends ApplicationEvent {

    public CredentialPolicyChangedSpringEvent(Object source) {
        super(source);
    }
}
