package com.ingot.cloud.pms.social;

import com.ingot.cloud.pms.api.model.domain.SysUser;

/**
 * <p>Description  : SocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:47 上午.</p>
 */
public interface SocialProcessor {

    /**
     * 执行社交流程，获取用户信息
     *
     * @param code 获取用户信息详情数据
     * @return 渠道唯一ID
     */
    String uniqueID(String code);

    /**
     * 根据OpenId获取用户信息
     *
     * @param uniqueID 渠道唯一ID
     * @return {@link SysUser}
     */
    SysUser info(String uniqueID);

    /**
     * 用户绑定uniqueID
     *
     * @param user     {@link SysUser}
     * @param uniqueID 渠道唯一ID
     */
    void bind(SysUser user, String uniqueID);
}
