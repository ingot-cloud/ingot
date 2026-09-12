package com.ingot.framework.data.mybatis.scope.authorization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingot.framework.eventbus.EventType;
import com.ingot.framework.eventbus.InvalidationEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>授权快照跨节点失效事件。当前按全量清理热缓存，期限提供最坏时效边界。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EventType("authorization.invalidate")
public class AuthorizationInvalidationEvent extends InvalidationEvent {

    /**
     * 是否全量失效。
     */
    private boolean all;

    @JsonCreator
    public AuthorizationInvalidationEvent(@JsonProperty("all") boolean all) {
        this.all = all;
    }

    /**
     * 构造全量失效事件。
     *
     * @return 全量失效事件
     */
    public static AuthorizationInvalidationEvent all() {
        return new AuthorizationInvalidationEvent(true);
    }
}
