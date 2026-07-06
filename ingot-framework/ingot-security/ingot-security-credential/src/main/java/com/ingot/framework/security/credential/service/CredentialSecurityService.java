package com.ingot.framework.security.credential.service;

import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;

/**
 * 凭证安全服务
 * <p>统一的凭证校验入口，封装所有校验逻辑</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
public interface CredentialSecurityService {

    /**
     * 校验凭证
     * <p>根据场景自动查询历史密码、过期信息，并进行校验</p>
     *
     * @param request 校验请求
     * @return 校验结果
     */
    PasswordCheckResult validate(CredentialValidateRequest request);

    /**
     * 保存密码历史
     * <p>在修改密码成功后调用</p>
     *
     * @param userId   用户ID
     * @param password 密码
     */
    void savePasswordHistory(Long userId, String password);

    /**
     * 更新密码过期时间
     * <p>在修改密码成功后调用</p>
     *
     * @param userId 用户ID
     */
    void updatePasswordExpiration(Long userId);
}
