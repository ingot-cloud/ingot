package com.ingot.framework.account.web.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ingot.framework.account.domain.config.AccountDomainProperties;
import com.ingot.framework.account.domain.model.LockState;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUserMetaKeys;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import lombok.extern.slf4j.Slf4j;

/**
 * 面向 PMS / Member 等“账号体系服务”的登录认证上下文填充工具。
 * <p>
 * 仅在用户名/密码登录场景（{@code /inner/user/details}）调用一次，负责把账号的
 * “硬过期决策 + 软上下文 meta” 同步到 {@link UserDetailsResponse}，供 Auth 侧的
 * {@code InUserDetailsChecker} / {@code DefaultUserCredentialChecker} 做精细化决策。
 * </p>
 *
 * <h3>填充语义</h3>
 * <ol>
 *   <li><b>硬决策</b>（阻断登录）：密码硬过期时将 {@code credentialsNonExpired=false}，
 *       Auth 侧会直接抛出 {@code CredentialsExpiredException}</li>
 *   <li><b>软上下文</b>（辅助决策）：通过 {@code meta} 传递锁定时间、失败计数、阈值等，
 *       Auth 侧据此生成友好提示。key 定义见 {@link InUserMetaKeys}</li>
 * </ol>
 *
 * <h3>可选依赖（按需降级）</h3>
 * <ul>
 *   <li>{@code credentialSecurityService} 为 {@code null} 或业务侧未启用密码策略时：跳过硬过期判定</li>
 *   <li>{@code lockStatePort} 为 {@code null}、{@code accountProperties} 未启用锁定策略时：跳过 meta 填充</li>
 * </ul>
 * <p>这使得 Member 等 baseline 场景（不启用密码过期/自动锁定策略）可以不注入上述依赖，
 * 调用方仍可安全使用本工具，不会导致启动失败或空指针。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // PMS / Member IdentityResolver 登录场景
 * UserDetailsResponse response = identityMap(user);
 * authContextSupport.fill(response, userId, userType);
 * return response;
 * }</pre>
 *
 * @author jymot
 * @since 2026-02-14
 */
@Slf4j
public class AuthContextSupport {

    private final CredentialSecurityService credentialSecurityService;
    private final LockStatePort lockStatePort;
    private final AccountDomainProperties accountProperties;

    public AuthContextSupport(CredentialSecurityService credentialSecurityService,
                              LockStatePort lockStatePort,
                              AccountDomainProperties accountProperties) {
        this.credentialSecurityService = credentialSecurityService;
        this.lockStatePort = lockStatePort;
        this.accountProperties = accountProperties;
    }

    /**
     * 填充认证上下文到 {@link UserDetailsResponse}
     * <p>宽限期 / 即将过期属于用户感知提示，不走此方法，应由登录后的用户信息接口单独返回。</p>
     *
     * @param result   待填充的响应对象，为 {@code null} 时直接返回
     * @param userId   用户 ID
     * @param userType 用户类型
     */
    public void fill(UserDetailsResponse result, Long userId, UserTypeEnum userType) {
        if (result == null) {
            return;
        }

        // 1. 硬过期判定：仅在账号可登录（已启用且未锁定）且注入了凭证服务时执行
        if (credentialSecurityService != null
                && Boolean.TRUE.equals(result.getEnabled())
                && !Boolean.TRUE.equals(result.getLocked())) {
            try {
                PasswordCheckResult checkResult = credentialSecurityService.validate(
                        CredentialValidateRequest.builder()
                                .scene(CredentialScene.LOGIN)
                                .userId(userId)
                                .manualProcessError(true)
                                .build());
                if (!checkResult.isPassed()) {
                    result.setCredentialsNonExpired(false);
                }
            } catch (Exception e) {
                log.warn("[AuthContextSupport] 凭证过期检查异常，放行登录 userId={}", userId, e);
            }
        }

        // 2. 软上下文：仅当锁定策略开启 + 端口可用时填充，否则 meta 为空、不设置
        Map<String, Object> meta = buildMeta(userId, userType, result);
        if (!meta.isEmpty()) {
            result.setMeta(meta);
        }
    }

    private Map<String, Object> buildMeta(Long userId, UserTypeEnum userType, UserDetailsResponse result) {
        Map<String, Object> meta = new HashMap<>(4);

        AccountDomainProperties.LockoutPolicy policy =
                accountProperties != null ? accountProperties.getLockout() : null;
        if (policy != null && policy.isEnabled()) {
            meta.put(InUserMetaKeys.MAX_FAILED_ATTEMPTS, policy.getMaxAttempts());
            meta.put(InUserMetaKeys.HINT_AFTER_ATTEMPTS, policy.getHintAfterAttempts());
        }

        if (lockStatePort == null) {
            return meta;
        }

        try {
            Optional<LockState> lockStateOpt = lockStatePort.findByUser(userId, userType);
            if (lockStateOpt.isPresent()) {
                LockState lockState = lockStateOpt.get();
                // 锁定状态下若为临时锁定，传递到期时间（ISO-8601 字符串，避免 Jackson 序列化差异）
                if (Boolean.TRUE.equals(result.getLocked()) && lockState.getLockedUntil() != null) {
                    meta.put(InUserMetaKeys.LOCKED_UNTIL, lockState.getLockedUntil().toString());
                }
                // 未锁定时传递当前失败次数，供 DefaultUserCredentialChecker 做分级提示
                if (!Boolean.TRUE.equals(result.getLocked())
                        && lockState.getFailedLoginCount() != null) {
                    meta.put(InUserMetaKeys.FAILED_LOGIN_COUNT, lockState.getFailedLoginCount());
                }
            }
        } catch (Exception e) {
            log.warn("[AuthContextSupport] 查询锁定状态异常，跳过 meta 填充 userId={}", userId, e);
        }

        return meta;
    }
}
