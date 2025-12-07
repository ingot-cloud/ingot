package com.ingot.framework.social.common.event;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <p>Description  : 社交配置变更事件.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 14:05.</p>
 */
@Getter
public class SocialConfigChangedEvent extends ApplicationEvent {
    /**
     * 社交类型
     */
    private final SocialTypeEnum socialType;
    
    /**
     * 变更类型
     */
    private final ConfigChangeType changeType;

    /**
     * 应用ID（可选，用于单个配置的变更）
     */
    private final String appId;

    /**
     * 构造函数 - 用于全量刷新
     *
     * @param source 事件源
     * @param socialType 社交类型
     */
    public SocialConfigChangedEvent(Object source, SocialTypeEnum socialType) {
        super(source);
        this.socialType = socialType;
        this.changeType = ConfigChangeType.REFRESH_ALL;
        this.appId = null;
    }

    /**
     * 构造函数 - 用于单个配置的变更
     *
     * @param source     事件源
     * @param socialType 社交类型
     * @param changeType 变更类型
     * @param appId      应用ID
     */
    public SocialConfigChangedEvent(Object source, SocialTypeEnum socialType, 
                                   ConfigChangeType changeType, String appId) {
        super(source);
        this.socialType = socialType;
        this.changeType = changeType;
        this.appId = appId;
    }

    /**
     * 配置变更类型
     */
    public enum ConfigChangeType {
        /**
         * 添加配置
         */
        ADD,
        /**
         * 更新配置
         */
        UPDATE,
        /**
         * 删除配置
         */
        DELETE,
        /**
         * 刷新所有配置
         */
        REFRESH_ALL
    }
}

