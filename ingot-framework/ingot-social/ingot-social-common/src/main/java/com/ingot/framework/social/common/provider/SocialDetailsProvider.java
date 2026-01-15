package com.ingot.framework.social.common.provider;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;

/**
 * <p>Description  : 社交详情提供者接口.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 18:00.</p>
 */
public interface SocialDetailsProvider {

    /**
     * 根据社交类型获取所有社交详情
     *
     * @param socialType 社交类型
     * @return 社交详情列表，如果获取失败返回空列表
     */
    List<SysSocialDetails> getDetailsByType(SocialTypeEnum socialType);

    /**
     * 根据AppId获取社交详情
     *
     * @param appId 应用ID
     * @return 社交详情，如果不存在返回null
     */
    SysSocialDetails getDetailsByAppId(String appId);

    /**
     * 检查提供者是否可用
     *
     * @return true-可用，false-不可用
     */
    default boolean isAvailable() {
        return true;
    }
}

