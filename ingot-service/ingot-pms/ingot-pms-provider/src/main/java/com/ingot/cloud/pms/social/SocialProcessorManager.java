package com.ingot.cloud.pms.social;

import com.ingot.framework.core.model.enums.SocialTypeEnums;

/**
 * <p>Description  : SocialProcessorManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:41 AM.</p>
 */
public interface SocialProcessorManager {

    /**
     * 执行社交流程，获取用户信息
     *
     * @param code 获取用户信息详情数据
     * @return 渠道唯一ID
     */
    String getUniqueID(SocialTypeEnums socialType, String code);

    /**
     * 根据OpenId获取用户信息
     *
     * @param uniqueID 渠道唯一ID
     * @return 返回用户信息
     */
    <T> T getUserInfo(SocialTypeEnums socialType, String uniqueID);

    /**
     * 用户绑定uniqueID
     *
     * @param user     用户信息
     * @param uniqueID 渠道唯一ID
     */
    <T> void bind(SocialTypeEnums socialType, T user, String uniqueID);
}
