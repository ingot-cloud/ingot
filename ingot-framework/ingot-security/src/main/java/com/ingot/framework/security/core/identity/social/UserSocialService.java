package com.ingot.framework.security.core.identity.social;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;

/**
 * <p>Description  : SocialResolverService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:03.</p>
 */
public interface UserSocialService {
    /**
     * 获取用户渠道唯一ID
     *
     * @param code 社交登录code
     * @return 渠道唯一ID
     */
    String getUniqueID(SocialTypeEnum socialType, String code);

    /**
     * 根据OpenId获取用户信息
     *
     * @param uniqueID 渠道唯一ID
     * @return 返回用户信息
     */
    <T> T getUserInfo(SocialTypeEnum socialType, String uniqueID);

    /**
     * 用户绑定uniqueID
     *
     * @param user     用户信息
     * @param uniqueID 渠道唯一ID
     */
    <T> void bind(SocialTypeEnum socialType, T user, String uniqueID);
}
