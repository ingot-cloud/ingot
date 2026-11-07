package com.ingot.cloud.iam.application.social;

import java.util.List;

import com.ingot.cloud.iam.api.model.dto.social.SocialDetailsDTO;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;

/**
 * <p>Description  : SocialDetailsQuery.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 16:04.</p>
 */
public interface SocialDetailsQuery {

    /**
     * 根据类型获取社交信息
     *
     * @param type {@link SocialTypeEnum}
     * @return {@link SocialDetailsDTO}
     */
    List<SocialDetailsDTO> getSocialDetailsByType(SocialTypeEnum type);

    /**
     * 根据appId获取社交信息
     *
     * @param appId appId
     * @return {@link SocialDetailsDTO}
     */
    SocialDetailsDTO getSocialDetailsByAppId(String appId);
}
