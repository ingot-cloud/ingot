package com.ingot.framework.security.credential.validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认密码校验器实现
 * <p>统一的密码校验入口，按优先级和场景依次执行各策略</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPasswordValidator implements PasswordValidator {
    private final CredentialPolicyLoader loader;

    @Override
    public PasswordCheckResult validate(PolicyCheckContext context) {
        // 按优先级排序策略
        List<PasswordPolicy> sortedPolicies = new ArrayList<>(loader.loadPolicies());
        sortedPolicies.sort(Comparator.comparingInt(PasswordPolicy::getPriority));

        PasswordCheckResult finalResult = PasswordCheckResult.pass();

        for (PasswordPolicy policy : sortedPolicies) {
            // 检查策略是否适用于当前场景
            if (!policy.isApplicableToScene(context.getScene())) {
                log.debug("策略 {} 不适用于场景 {}", policy.getType().getText(), context.getScene());
                continue;
            }

            log.debug("执行策略 {} - 场景: {}", policy.getType().getText(), context.getScene());

            // 执行策略校验
            PasswordCheckResult policyResult = policy.check(context);

            // 合并警告信息
            if (policyResult.hasWarnings()) {
                finalResult.getWarnings().addAll(policyResult.getWarnings());
            }

            // 合并元数据
            if (!policyResult.getMetadata().isEmpty()) {
                finalResult.getMetadata().putAll(policyResult.getMetadata());
            }

            if (!policyResult.isPassed()) {
                // 合并失败原因
                finalResult.setPassed(false);
                finalResult.getFailureReasons().addAll(policyResult.getFailureReasons());

                log.warn("策略 {} 校验失败: {}", policy.getType().getText(), policyResult.getFailureMessage());

                // 如果是阻断式策略且失败，停止后续校验
                if (policy.isBlocking()) {
                    log.debug("策略 {} 为阻断式策略，停止后续校验", policy.getType().getText());
                    break;
                }
            }
        }

        return finalResult;
    }
}
