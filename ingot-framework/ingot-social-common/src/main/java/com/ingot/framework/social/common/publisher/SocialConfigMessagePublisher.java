package com.ingot.framework.social.common.publisher;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.event.SocialConfigRedisMessage;

/**
 * <p>Description  : 社交配置消息发布器接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 14:20.</p>
 */
public interface SocialConfigMessagePublisher {

    /**
     * 发布刷新所有配置的消息
     *
     * @param socialType 社交类型
     */
    void publishRefreshAll(SocialTypeEnum socialType);

    /**
     * 发布添加配置的消息
     *
     * @param socialType 社交类型
     * @param appId 应用ID
     */
    void publishAdd(SocialTypeEnum socialType, String appId);

    /**
     * 发布更新配置的消息
     *
     * @param socialType 社交类型
     * @param appId 应用ID
     */
    void publishUpdate(SocialTypeEnum socialType, String appId);

    /**
     * 发布删除配置的消息
     *
     * @param socialType 社交类型
     * @param appId 应用ID
     */
    void publishDelete(SocialTypeEnum socialType, String appId);

    /**
     * 发布消息
     *
     * @param message 消息对象
     */
    void publish(SocialConfigRedisMessage message);
}

