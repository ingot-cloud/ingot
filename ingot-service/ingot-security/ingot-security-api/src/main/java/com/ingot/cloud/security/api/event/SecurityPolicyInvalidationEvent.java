package com.ingot.cloud.security.api.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingot.framework.eventbus.EventType;
import com.ingot.framework.eventbus.InvalidationEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 安全策略缓存失效事件。
 *
 * <p>发布场景：ingot-service-security 在 {@code @TransactionalEventListener(AFTER_COMMIT)}
 * 中由 Publisher 触发，订阅方（网关 / 业务侧）的 {@code SecurityPolicyCacheCoordinator}
 * 据此清空对应域的 L1 编译缓存，下次访问时从 yaml（local）或 Inner Feign（remote）
 * 重新加载并编译。</p>
 *
 * <p>语义：按 {@link SecurityPolicyDomain} 做域级失效；{@link SecurityPolicyDomain#ALL}
 * 表示清空全部域。当前不支持单条 ID 精细化失效，规则量较小时按域整体重建已足够。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EventType("security-policy.invalidate")
public class SecurityPolicyInvalidationEvent extends InvalidationEvent {

    private SecurityPolicyDomain domain;

    @JsonCreator
    public SecurityPolicyInvalidationEvent(@JsonProperty("domain") SecurityPolicyDomain domain) {
        this.domain = domain;
    }

    public static SecurityPolicyInvalidationEvent of(SecurityPolicyDomain domain) {
        return new SecurityPolicyInvalidationEvent(domain);
    }

    public static SecurityPolicyInvalidationEvent all() {
        return new SecurityPolicyInvalidationEvent(SecurityPolicyDomain.ALL);
    }
}
