package com.ingot.framework.security.credential.service;

import java.time.LocalDateTime;

/**
 * 初始密码服务，按策略生成初始密码并判定初始密码有效期。
 *
 * <p>用于管理员创建账号或重置密码场景：根据 {@code ingot.security.credential.policy.initial-password}
 * 配置生成随机或统一默认密码，并提供初始密码是否已过期的判定，配合账号域 {@code mustChangePwd}
 * 实现首登强制改密与用后失效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface InitialPasswordService {

    /**
     * 按当前策略生成一个初始密码明文。
     *
     * @return 初始密码明文
     */
    String generate();

    /**
     * 判断初始密码是否已超过有效期。
     * <p>策略 {@code validHours=0} 表示不限制，恒返回 {@code false}。</p>
     *
     * @param issuedAt 初始密码签发时间（通常为 {@code passwordChangedAt}）
     * @return {@code true} 表示已失效需重新重置
     */
    boolean isExpired(LocalDateTime issuedAt);

    /**
     * 首次登录是否要求强制修改密码。
     *
     * @return 是否强制修改
     */
    boolean isForceChangeOnFirstLogin();
}
