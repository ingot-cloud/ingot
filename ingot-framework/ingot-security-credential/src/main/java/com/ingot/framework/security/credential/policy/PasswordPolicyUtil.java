package com.ingot.framework.security.credential.policy;

import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordPolicyUtil
 *
 * @author jy
 * @since 2026/1/30
 */
public class PasswordPolicyUtil {

    /**
     * 创建密码强度策略
     */
    public static PasswordStrengthPolicy createStrengthPolicy(Map<String, Object> config, int priority) {
        PasswordStrengthPolicy policy = new PasswordStrengthPolicy() {
            @Override
            public int getPriority() {
                return priority;
            }
        };

        // 设置配置参数
        if (config.containsKey("minLength")) {
            policy.setMinLength(((Number) config.get("minLength")).intValue());
        }
        if (config.containsKey("maxLength")) {
            policy.setMaxLength(((Number) config.get("maxLength")).intValue());
        }
        if (config.containsKey("requireUppercase")) {
            policy.setRequireUppercase((Boolean) config.get("requireUppercase"));
        }
        if (config.containsKey("requireLowercase")) {
            policy.setRequireLowercase((Boolean) config.get("requireLowercase"));
        }
        if (config.containsKey("requireDigit")) {
            policy.setRequireDigit((Boolean) config.get("requireDigit"));
        }
        if (config.containsKey("requireSpecialChar")) {
            policy.setRequireSpecialChar((Boolean) config.get("requireSpecialChar"));
        }
        if (config.containsKey("specialChars")) {
            policy.setSpecialChars((String) config.get("specialChars"));
        }
        if (config.containsKey("forbiddenPatterns")) {
            @SuppressWarnings("unchecked")
            List<String> patterns = (List<String>) config.get("forbiddenPatterns");
            policy.setForbiddenPatterns(patterns);
        }
        if (config.containsKey("forbidUserAttributes")) {
            policy.setForbidUserAttributes((Boolean) config.get("forbidUserAttributes"));
        }

        return policy;
    }

    /**
     * 创建密码历史策略
     */
    public static PasswordHistoryPolicy createHistoryPolicy(Map<String, Object> config, int priority, PasswordEncoder passwordEncoder) {
        PasswordHistoryPolicy policy = new PasswordHistoryPolicy(passwordEncoder) {
            @Override
            public int getPriority() {
                return priority;
            }
        };

        // 设置配置参数
        if (config.containsKey("enabled")) {
            policy.setEnabled((Boolean) config.get("enabled"));
        }
        if (config.containsKey("checkCount")) {
            policy.setCheckCount(((Number) config.get("checkCount")).intValue());
        }

        return policy;
    }

    /**
     * 创建密码过期策略
     */
    public static PasswordExpirationPolicy createExpirationPolicy(Map<String, Object> config, int priority) {
        PasswordExpirationPolicy policy = new PasswordExpirationPolicy();
        policy.setPriority(priority);

        // 设置配置参数
        if (config.containsKey("enabled")) {
            policy.setEnabled((Boolean) config.get("enabled"));
        }
        if (config.containsKey("maxDays")) {
            policy.setMaxDays(((Number) config.get("maxDays")).intValue());
        }
        if (config.containsKey("warningDaysBefore")) {
            policy.setWarningDaysBefore(((Number) config.get("warningDaysBefore")).intValue());
        }
        if (config.containsKey("graceLoginCount")) {
            policy.setGraceLoginCount(((Number) config.get("graceLoginCount")).intValue());
        }

        return policy;
    }

}
