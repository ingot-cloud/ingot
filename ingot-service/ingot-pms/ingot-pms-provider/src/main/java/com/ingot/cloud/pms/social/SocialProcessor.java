package com.ingot.cloud.pms.social;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;

/**
 * <p>Description  : SocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:47 上午.</p>
 */
public interface SocialProcessor<T> {

    /**
     * 是否支持该流程
     *
     * @param socialType {@link SocialTypeEnum}
     * @return Boolean
     */
    boolean support(SocialTypeEnum socialType);

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
