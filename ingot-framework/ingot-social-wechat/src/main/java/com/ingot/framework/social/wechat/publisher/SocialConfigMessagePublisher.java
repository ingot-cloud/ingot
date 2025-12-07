package com.ingot.framework.social.wechat.publisher;

import com.ingot.framework.social.wechat.event.SocialConfigRedisMessage;

/**
 * <p>Description  : 社交配置消息发布器接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 10:00.</p>
 */
public interface SocialConfigMessagePublisher {

    /**
     * 发布刷新所有配置的消息
     */
    void publishRefreshAll();

    /**
     * 发布添加配置的消息
     *
     * @param appId 应用ID
     */
    void publishAdd(String appId);

    /**
     * 发布更新配置的消息
     *
     * @param appId 应用ID
     */
    void publishUpdate(String appId);

    /**
     * 发布删除配置的消息
     *
     * @param appId 应用ID
     */
    void publishDelete(String appId);

    /**
     * 发布消息
     *
     * @param message 消息对象
     */
    void publish(SocialConfigRedisMessage message);
}

