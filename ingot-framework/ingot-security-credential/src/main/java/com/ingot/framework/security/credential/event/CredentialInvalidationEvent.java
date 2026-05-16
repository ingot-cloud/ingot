package com.ingot.framework.security.credential.event;

import com.ingot.framework.eventbus.EventType;
import com.ingot.framework.eventbus.InvalidationEvent;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 凭证策略缓存失效事件。当前只支持「全量失效」，所有节点收到后清空本节点的
 * L1 Caffeine、L2 Redis（实际由发起节点已清，订阅端兜底）以及编译后的策略列表。
 *
 * @author jy
 * @since 2026/5/16
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EventType("credential.invalidate")
public class CredentialInvalidationEvent extends InvalidationEvent {

    public static CredentialInvalidationEvent all() {
        return new CredentialInvalidationEvent();
    }
}
