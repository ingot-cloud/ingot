package com.ingot.framework.social.wechat.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>Description  : 社交配置变更Redis消息.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 17:20.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialConfigRedisMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 创建刷新所有配置的消息
     */
    public static SocialConfigRedisMessage refreshAll() {
        return new SocialConfigRedisMessage(
                SocialConfigChangedEvent.ConfigChangeType.REFRESH_ALL.name(),
                null,
                System.currentTimeMillis()
        );
    }

    /**
     * 创建添加配置的消息
     */
    public static SocialConfigRedisMessage add(String appId) {
        return new SocialConfigRedisMessage(
                SocialConfigChangedEvent.ConfigChangeType.ADD.name(),
                appId,
                System.currentTimeMillis()
        );
    }

    /**
     * 创建更新配置的消息
     */
    public static SocialConfigRedisMessage update(String appId) {
        return new SocialConfigRedisMessage(
                SocialConfigChangedEvent.ConfigChangeType.UPDATE.name(),
                appId,
                System.currentTimeMillis()
        );
    }

    /**
     * 创建删除配置的消息
     */
    public static SocialConfigRedisMessage delete(String appId) {
        return new SocialConfigRedisMessage(
                SocialConfigChangedEvent.ConfigChangeType.DELETE.name(),
                appId,
                System.currentTimeMillis()
        );
    }
}


