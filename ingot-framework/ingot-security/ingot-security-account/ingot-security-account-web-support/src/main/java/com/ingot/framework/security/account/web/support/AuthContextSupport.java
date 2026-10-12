package com.ingot.framework.security.account.web.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ingot.framework.commons.model.security.CredentialExpirationReason;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import com.ingot.framework.security.core.userdetails.InUserMetaKeys;
import com.ingot.framework.security.credential.model.CredentialErrorCode;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>填充 IAM / Member 登录认证状态、过期提示原因与锁定上下文。</p>
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
 *   <li>{@code lockStatePort} 为 {@code null}、{@code lockoutPolicyLoader} 未启用锁定策略时：跳过 meta 填充</li>
 * </ul>
 * <p>这使得 Member 等 baseline 场景（不启用密码过期/自动锁定策略）可以不注入上述依赖，
 * 调用方仍可安全使用本工具，不会导致启动失败或空指针。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // IAM / Member IdentityResolver 登录场景
 * UserDetailsResponse response = identityMap(user);
 * authContextSupport.fill(response, userId, userType);
 * return response;
 * }</pre>
 *
 * @author jymot
 * @since 2026-02-14
 */
@Slf4j
@RequiredArgsConstructor
public class AuthContextSupport {

    private final CredentialSecurityService credentialSecurityService;
    private final LockStatePort lockStatePort;
    private final AccountLockoutPolicyLoader lockoutPolicyLoader;
    private final InitialPasswordService initialPasswordService;
    private final UserAccountPort userAccountPort;

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

        Map<String, Object> meta = new HashMap<>();
        if (result.getMeta() != null) {
            meta.putAll(result.getMeta());
        }
        CredentialExpirationReason expirationReason = null;

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
                    expirationReason = checkResult.getFailureCode() == CredentialErrorCode.EXPIRED
                            ? CredentialExpirationReason.PASSWORD_EXPIRED : CredentialExpirationReason.UNKNOWN;
                }
            } catch (Exception e) {
                log.warn("[AuthContextSupport] 凭证过期检查异常，放行登录 userId={}", userId, e);
            }

            // 1.1 初始密码硬超期：持有初始/重置密码（mustChangePwd=true）且超过 validHours 未改密，
            //     视为初始密码失效，硬阻断登录，需管理员重置。
            // 普通校验已阻断时仍识别初始密码原因；只补充提示分类，不解除已有阻断。
            if (isInitialPasswordExpired(userId, userType)) {
                result.setCredentialsNonExpired(false);
                expirationReason = CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED;
            }
        }

        // 2. 合并锁定上下文和本次过期原因，保留已有的其他 meta。
        meta.putAll(buildMeta(userId, userType, result));
        if (Boolean.FALSE.equals(result.getCredentialsNonExpired())) {
            if (expirationReason != null) {
                meta.put(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON, expirationReason.getValue());
            } else {
                meta.putIfAbsent(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON,
                        CredentialExpirationReason.UNKNOWN.getValue());
            }
        } else {
            meta.remove(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON);
        }
        if (!meta.isEmpty() || result.getMeta() != null) {
            result.setMeta(meta);
        }
    }

    /**
     * 初始密码 {@code validHours} 硬超期判定，用于阻断及优先选择提示原因。
     * <p>依赖可选注入：缺少 {@link InitialPasswordService} 或 {@link UserAccountPort} 时跳过（降级）。</p>
     */
    private boolean isInitialPasswordExpired(Long userId, UserTypeEnum userType) {
        if (initialPasswordService == null || userAccountPort == null) {
            return false;
        }
        try {
            UserAccount account = userAccountPort.findById(userId, userType).orElse(null);
            if (account == null || !Boolean.TRUE.equals(account.getMustChangePwd())) {
                return false;
            }
            if (initialPasswordService.isExpired(account.getPasswordChangedAt())) {
                log.info("[AuthContextSupport] 初始密码已超期，阻断登录并需管理员重置 userId={}", userId);
                return true;
            }
        } catch (Exception e) {
            log.warn("[AuthContextSupport] 初始密码超期检查异常，放行登录 userId={}", userId, e);
        }
        return false;
    }

    private Map<String, Object> buildMeta(Long userId, UserTypeEnum userType, UserDetailsResponse result) {
        Map<String, Object> meta = new HashMap<>(4);

        LockoutPolicy policy = lockoutPolicyLoader != null
                ? lockoutPolicyLoader.getLockoutPolicy(userType) : null;
        if (policy != null && policy.enabled()) {
            meta.put(InUserMetaKeys.MAX_FAILED_ATTEMPTS, policy.maxAttempts());
            meta.put(InUserMetaKeys.HINT_AFTER_ATTEMPTS, policy.hintAfterAttempts());
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
