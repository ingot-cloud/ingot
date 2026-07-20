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

    /**
     * 标记 / 清除强制修改密码（凭证域），与账号域 {@code mustChangePwd} 语义保持一致。
     * <p>管理员创建 / 重置密码时置位，用户改密成功后清除。</p>
     *
     * @param userId      用户ID
     * @param forceChange 是否强制修改
     */
    void markForceChange(Long userId, boolean forceChange);

    /**
     * 登录成功后消费一次宽限登录次数。
     * <p>仅当过期策略启用且密码已过期（处于宽限期）时扣减，其余情况不处理。</p>
     *
     * @param userId 用户ID
     * @return 剩余宽限次数；未处于宽限扣减场景返回 {@code -1}
     */
    int consumeGraceLoginOnSuccess(Long userId);
}
