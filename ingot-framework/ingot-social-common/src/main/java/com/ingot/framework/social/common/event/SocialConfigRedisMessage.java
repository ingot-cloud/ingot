package com.ingot.framework.social.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>Description  : 社交配置变更Redis消息.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 14:10.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialConfigRedisMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 社交类型
     */
    private String socialType;
    
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
    public static SocialConfigRedisMessage refreshAll(String socialType) {
        return new SocialConfigRedisMessage(
                socialType,
                SocialConfigChangedEvent.ConfigChangeType.REFRESH_ALL.name(),
                null,
                System.currentTimeMillis()
        );
    }

    /**
     * 创建添加配置的消息
     */
    public static SocialConfigRedisMessage add(String socialType, String appId) {
        return new SocialConfigRedisMessage(
                socialType,
                SocialConfigChangedEvent.ConfigChangeType.ADD.name(),
                appId,
                System.currentTimeMillis()
        );
    }

    /**
     * 创建更新配置的消息
     */
    public static SocialConfigRedisMessage update(String socialType, String appId) {
        return new SocialConfigRedisMessage(
                socialType,
                SocialConfigChangedEvent.ConfigChangeType.UPDATE.name(),
                appId,
                System.currentTimeMillis()
        );
    }

    /**
     * 创建删除配置的消息
     */
    public static SocialConfigRedisMessage delete(String socialType, String appId) {
        return new SocialConfigRedisMessage(
                socialType,
                SocialConfigChangedEvent.ConfigChangeType.DELETE.name(),
                appId,
                System.currentTimeMillis()
        );
    }
}

