package com.ingot.framework.social.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingot.framework.eventbus.EventType;
import com.ingot.framework.eventbus.InvalidationEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 社交配置跨节点失效事件，经 {@link com.ingot.framework.eventbus.InvalidationBus} 广播。
 * <p>
 * 载荷字段与历史 {@link SocialConfigRedisMessage} 对齐，便于兼容既有调用方。
 * </p>
 *
 * @author jy
 * @since 2026/5/13
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EventType("social.invalidate")
public class SocialInvalidationEvent extends InvalidationEvent {

    private String socialType;

    private String changeType;

    private String appId;

    @JsonCreator
    public SocialInvalidationEvent(@JsonProperty("socialType") String socialType,
                                   @JsonProperty("changeType") String changeType,
                                   @JsonProperty("appId") String appId) {
        this.socialType = socialType;
        this.changeType = changeType;
        this.appId = appId;
    }

    public static SocialInvalidationEvent refreshAll(String socialTypeValue) {
        return new SocialInvalidationEvent(
                socialTypeValue,
                SocialConfigChangedEvent.ConfigChangeType.REFRESH_ALL.name(),
                null);
    }

    public static SocialInvalidationEvent add(String socialTypeValue, String appId) {
        return new SocialInvalidationEvent(
                socialTypeValue,
                SocialConfigChangedEvent.ConfigChangeType.ADD.name(),
                appId);
    }

    public static SocialInvalidationEvent update(String socialTypeValue, String appId) {
        return new SocialInvalidationEvent(
                socialTypeValue,
                SocialConfigChangedEvent.ConfigChangeType.UPDATE.name(),
                appId);
    }

    public static SocialInvalidationEvent delete(String socialTypeValue, String appId) {
        return new SocialInvalidationEvent(
                socialTypeValue,
                SocialConfigChangedEvent.ConfigChangeType.DELETE.name(),
                appId);
    }

    /**
     * 与历史 {@link SocialConfigRedisMessage} 互转（字段一一对应）。
     */
    public static SocialInvalidationEvent fromRedisMessage(SocialConfigRedisMessage message) {
        if (message == null) {
            return null;
        }
        return new SocialInvalidationEvent(message.getSocialType(), message.getChangeType(), message.getAppId());
    }

    public SocialConfigRedisMessage toRedisMessage() {
        return new SocialConfigRedisMessage(socialType, changeType, appId,
                getTimestamp() > 0 ? getTimestamp() : System.currentTimeMillis());
    }
}
