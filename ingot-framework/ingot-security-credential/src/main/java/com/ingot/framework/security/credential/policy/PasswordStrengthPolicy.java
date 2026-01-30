package com.ingot.framework.security.credential.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 密码强度策略
 * <p>校验密码的长度、复杂度等基本要求</p>
 * <p>适用场景：注册、修改密码、重置密码</p>
 *
 * @author jymot
 * @since 2026-01-21
 */
@Setter
@Getter
public class PasswordStrengthPolicy implements PasswordPolicy {

    private static final String NAME = "PASSWORD_STRENGTH";
    private static final int DEFAULT_PRIORITY = 10;

    // 配置参数
    private int minLength = 8;
    private int maxLength = 32;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecialChar = false;
    private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private List<String> forbiddenPatterns = new ArrayList<>(
            Arrays.asList("password", "123456", "admin", "qwerty")
    );
    private boolean forbidUserAttributes = true;

    // 正则模式（预编译）
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public boolean isBlocking() {
        return true; // 阻断式策略
    }

    @Override
    public Set<CredentialScene> getApplicableScenes() {
        // 适用于注册、修改密码、重置密码场景
        return Set.of(
                CredentialScene.REGISTER,
                CredentialScene.CHANGE_PASSWORD,
                CredentialScene.RESET_PASSWORD
        );
    }

    @Override
    public PasswordCheckResult check(PolicyCheckContext context) {
        String password = context.getPassword();

        if (!StringUtils.hasText(password)) {
            return PasswordCheckResult.fail("密码不能为空");
        }

        List<String> failureReasons = new ArrayList<>();

        // 1. 长度检查
        if (password.length() < minLength) {
            failureReasons.add(String.format("密码长度不足，至少需要%d个字符", minLength));
        }
        if (password.length() > maxLength) {
            failureReasons.add(String.format("密码长度过长，最多%d个字符", maxLength));
        }

        // 2. 字符类型检查
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            failureReasons.add("密码必须包含至少一个大写字母");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            failureReasons.add("密码必须包含至少一个小写字母");
        }

        if (requireDigit && !DIGIT_PATTERN.matcher(password).matches()) {
            failureReasons.add("密码必须包含至少一个数字");
        }

        if (requireSpecialChar) {
            Pattern specialPattern = Pattern.compile(".*[" + Pattern.quote(specialChars) + "].*");
            if (!specialPattern.matcher(password).matches()) {
                failureReasons.add("密码必须包含至少一个特殊字符");
            }
        }

        // 3. 禁止的模式检查
        String lowerPassword = password.toLowerCase();
        for (String pattern : forbiddenPatterns) {
            if (lowerPassword.contains(pattern.toLowerCase())) {
                failureReasons.add(String.format("密码不能包含弱密码模式: %s", pattern));
            }
        }

        // 4. 禁止包含用户属性
        if (forbidUserAttributes) {
            if (StringUtils.hasText(context.getUsername()) &&
                    lowerPassword.contains(context.getUsername().toLowerCase())) {
                failureReasons.add("密码不能包含用户名");
            }
            if (StringUtils.hasText(context.getPhone()) &&
                    password.contains(context.getPhone())) {
                failureReasons.add("密码不能包含手机号");
            }
            if (StringUtils.hasText(context.getEmail())) {
                String emailUser = context.getEmail().split("@")[0];
                if (lowerPassword.contains(emailUser.toLowerCase())) {
                    failureReasons.add("密码不能包含邮箱");
                }
            }
        }

        if (!failureReasons.isEmpty()) {
            return PasswordCheckResult.fail(failureReasons);
        }

        return PasswordCheckResult.pass();
    }
}
