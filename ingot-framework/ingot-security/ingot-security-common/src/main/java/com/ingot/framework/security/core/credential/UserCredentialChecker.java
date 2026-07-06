package com.ingot.framework.security.core.credential;

import com.ingot.framework.security.oauth2.core.OAuth2Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 凭证策略检查扩展接口
 * <p>
 * 在 PASSWORD 授权模式下，对用户凭证进行完整检查（密码验证 + 上层策略检查）。
 * 框架提供 {@link DefaultUserCredentialChecker} 作为默认实现，仅做密码匹配。
 * 服务层可继承 {@link DefaultUserCredentialChecker} 并在 {@code super.check()} 之后
 * 追加策略检查（如密码过期、强制改密等），注册为 Spring Bean 后由框架自动装配。
 *
 * @author jymot
 * @since 2026-02-13
 * @see DefaultUserCredentialChecker
 */
public interface UserCredentialChecker {

    /**
     * 执行凭证检查
     *
     * @param user  已加载的用户信息
     * @param token 当前认证令牌，包含 grant type 与原始凭证
     * @throws AuthenticationException 凭证校验未通过时抛出
     */
    void check(UserDetails user, OAuth2Authentication token) throws AuthenticationException;

    /**
     * 同步 PasswordEncoder（Provider 初始化后由框架调用）
     * <p>默认空实现；{@link DefaultUserCredentialChecker} 及其子类按需覆写</p>
     */
    default void setPasswordEncoder(PasswordEncoder passwordEncoder) {
    }
}
