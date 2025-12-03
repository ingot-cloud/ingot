package com.ingot.framework.security.core.identity.social;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;

/**
 * <p>Description  : SocialResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:01.</p>
 */
public interface UserSocialResolver<T> {
    /**
     * 是否支持该流程
     *
     * @param socialType {@link SocialTypeEnum}
     * @return Boolean
     */
    boolean supports(SocialTypeEnum socialType);

    /**
     * 执行社交流程，获取用户信息
     *
     * @param code 获取用户信息详情数据
     * @return 渠道唯一ID
     */
    String getUniqueID(String code);

    /**
     * 根据OpenId获取用户信息
     *
     * @param uniqueID 渠道唯一ID
     * @return {@link T}
     */
    T getUserInfo(String uniqueID);

    /**
     * 用户绑定uniqueID
     *
     * @param user     {@link T}
     * @param uniqueID 渠道唯一ID
     */
    void bind(T user, String uniqueID);
}
