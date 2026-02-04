package com.ingot.framework.security.credential.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import com.ingot.framework.security.credential.model.domain.PasswordExpiration;
import com.ingot.framework.security.credential.model.domain.PasswordHistory;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.policy.PasswordExpirationPolicy;
import com.ingot.framework.security.credential.policy.PasswordHistoryPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import com.ingot.framework.security.credential.validator.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 凭证安全服务默认实现
 * <p>提供本地校验能力，自动查询历史密码和过期信息</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCredentialSecurityService implements CredentialSecurityService {
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordExpirationService passwordExpirationService;
    private final CredentialSecurityProperties properties;
    private final CredentialPolicyLoader credentialPolicyLoader;

    @Override
    public PasswordCheckResult validate(CredentialValidateRequest request) {
        log.debug("开始校验凭证 - 场景: {}, 用户ID: {}", request.getScene(), request.getUserId());

        // 构建校验上下文
        PolicyCheckContext.PolicyCheckContextBuilder contextBuilder = PolicyCheckContext.builder()
                .scene(request.getScene())
                .password(request.getPassword())
                .username(request.getUsername())
                .phone(request.getPhone())
                .email(request.getEmail())
                .userType(request.getUserType())
                .userId(request.getUserId());

        // 根据场景查询需要的数据
        enrichContextByScene(request, contextBuilder);

        // 执行校验
        PolicyCheckContext context = contextBuilder.build();
        PasswordCheckResult result = passwordValidator.validate(context);

        log.debug("凭证校验完成 - 场景: {}, 结果: {}", request.getScene(), result.isPassed());

        // 注册和修改密码，如果校验通过，都需要保存历史密码，更新密码过期信息
        if (result.isPassed()
                && (request.getScene() == CredentialScene.CHANGE_PASSWORD
                || request.getScene() == CredentialScene.REGISTER)) {
            savePasswordHistory(request.getUserId(), request.getPassword());
            updatePasswordExpiration(request.getUserId());
        }

        // 失败直接抛出异常
        result.ifErrorThrow();

        return result;
    }

    /**
     * 根据场景enrichContext
     */
    private void enrichContextByScene(CredentialValidateRequest request, PolicyCheckContext.PolicyCheckContextBuilder contextBuilder) {
        CredentialScene scene = request.getScene();

        // 修改密码场景：查询历史密码
        if (scene == CredentialScene.CHANGE_PASSWORD && request.getUserId() != null) {
            int checkCount = properties.getPolicy().getHistory().getCheckCount();
            List<PasswordHistory> histories = passwordHistoryService.getRecentHistory(
                    request.getUserId(), checkCount);

            if (!histories.isEmpty()) {
                List<String> passwordHashes = histories.stream()
                        .map(PasswordHistory::getPasswordHash)
                        .collect(Collectors.toList());
                contextBuilder.oldPasswordHashes(passwordHashes);
                log.debug("查询到 {} 条历史密码", passwordHashes.size());
            }
        }

        // 登录场景：查询过期信息
        if (scene == CredentialScene.LOGIN && request.getUserId() != null) {
            PasswordExpiration expiration = passwordExpirationService.getByUserId(request.getUserId());

            if (expiration != null) {
                contextBuilder
                        .lastPasswordChangedAt(expiration.getLastChangedAt())
                        .graceLoginRemaining(expiration.getGraceLoginRemaining());
                log.debug("查询到过期信息 - 最后修改时间: {}", expiration.getLastChangedAt());
            }
        }
    }

    @Override
    public void savePasswordHistory(Long userId, String password) {
        log.debug("保存密码历史 - 用户ID: {}", userId);

        credentialPolicyLoader.loadPolicies().stream()
                .filter(policy -> policy instanceof PasswordHistoryPolicy)
                .map(policy -> (PasswordHistoryPolicy) policy)
                .findFirst()
                .ifPresent(policy -> {
                    int maxRecords = policy.getCheckCount();
                    passwordHistoryService.saveHistory(userId, password, maxRecords);
                });
    }

    @Override
    public void updatePasswordExpiration(Long userId) {
        log.debug("更新密码过期时间 - 用户ID: {}", userId);

        credentialPolicyLoader.loadPolicies().stream()
                .filter(policy -> policy instanceof PasswordExpirationPolicy)
                .map(policy -> (PasswordExpirationPolicy) policy)
                .findFirst()
                .ifPresent(policy -> {
                    int maxDays = policy.getMaxDays();
                    passwordExpirationService.updateLastChanged(userId, maxDays);
                });
    }
}
