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
     * @return {@link SysUser}
     */
    SysUser exec(String code);
}
