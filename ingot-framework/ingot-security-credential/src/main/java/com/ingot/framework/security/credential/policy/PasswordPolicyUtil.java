package com.ingot.framework.security.credential.policy;

import java.util.List;
import java.util.Map;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
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
            Object minLength = config.get("minLength");
            policy.setMinLength(NumberUtil.parseInt(StrUtil.toString(minLength)));
        }
        if (config.containsKey("maxLength")) {
            Object maxLength = config.get("maxLength");
            policy.setMaxLength(NumberUtil.parseInt(StrUtil.toString(maxLength)));
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
            Object checkCount = config.get("checkCount");
            policy.setCheckCount(NumberUtil.parseInt(StrUtil.toString(checkCount)));
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
            policy.setMaxDays(NumberUtil.parseInt(StrUtil.toString(config.get("maxDays"))));
        }
        if (config.containsKey("warningDaysBefore")) {
            policy.setWarningDaysBefore(NumberUtil.parseInt(StrUtil.toString(config.get("warningDaysBefore"))));
        }
        if (config.containsKey("graceLoginCount")) {
            policy.setGraceLoginCount(NumberUtil.parseInt(StrUtil.toString(config.get("graceLoginCount"))));
        }

        return policy;
    }

}
