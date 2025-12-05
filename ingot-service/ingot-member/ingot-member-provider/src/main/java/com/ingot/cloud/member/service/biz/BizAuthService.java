package com.ingot.cloud.member.service.biz;

import com.ingot.cloud.member.api.model.dto.user.MemberUserInfoDTO;
import com.ingot.framework.security.core.userdetails.InUser;

/**
 * <p>Description  : BizAuthService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 17:30.</p>
 */
public interface BizAuthService {

    /**
     * 通过 {@link InUser} 获取用户信息
     *
     * @param user {@link InUser} 当前登录用户
     * @return {@link MemberUserInfoDTO}
     */
    MemberUserInfoDTO getUserInfo(InUser user);
}
