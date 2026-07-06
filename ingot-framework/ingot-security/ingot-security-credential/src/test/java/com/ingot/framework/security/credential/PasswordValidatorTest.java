package com.ingot.framework.security.credential;

import java.util.List;

import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.policy.PasswordStrengthPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.validator.DefaultPasswordValidator;
import com.ingot.framework.security.credential.validator.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码校验器测试
 *
 * @author jymot
 * @since 2026-01-21
 */
class PasswordValidatorTest {

    private PasswordStrengthPolicy strengthPolicy;
    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        strengthPolicy = new PasswordStrengthPolicy();
        strengthPolicy.setMinLength(8);
        strengthPolicy.setRequireUppercase(true);
        strengthPolicy.setRequireLowercase(true);
        strengthPolicy.setRequireDigit(true);
        strengthPolicy.setRequireSpecialChar(false);

        validator = newValidator(strengthPolicy);
    }

    @Test
    void testValidPassword() {
        PolicyCheckContext context = registerContext("Test1234", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertTrue(result.isPassed(), "密码应该通过校验");
        assertTrue(result.getFailureReasons().isEmpty(), "不应该有失败原因");
    }

    @Test
    void testPasswordTooShort() {
        PolicyCheckContext context = registerContext("Test1", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("密码长度不足"), "应该包含长度不足的错误");
    }

    @Test
    void testPasswordNoUppercase() {
        PolicyCheckContext context = registerContext("test1234", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("大写字母"), "应该包含缺少大写字母的错误");
    }

    @Test
    void testPasswordNoLowercase() {
        PolicyCheckContext context = registerContext("TEST1234", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("小写字母"), "应该包含缺少小写字母的错误");
    }

    @Test
    void testPasswordNoDigit() {
        PolicyCheckContext context = registerContext("TestTest", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("数字"), "应该包含缺少数字的错误");
    }

    @Test
    void testPasswordContainsUsername() {
        // 测试包含用户名（默认开启 forbidUserAttributes）
        strengthPolicy.setForbidUserAttributes(true);
        PolicyCheckContext context = registerContext("Testuser1234", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("用户名"), "应该包含不能包含用户名的错误");
    }

    @Test
    void testPasswordWithForbiddenPattern() {
        // 测试禁止模式：需要在策略中显式设置 forbiddenPatterns
        strengthPolicy.setForbiddenPatterns(List.of("password"));
        PolicyCheckContext context = registerContext("Password123", "admin");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("弱密码模式"), "应该包含弱密码模式的错误");
    }

    @Test
    void testMultipleErrors() {
        PolicyCheckContext context = registerContext("test", "testuser");
        PasswordCheckResult result = validator.validate(context);

        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureReasons().size() > 1, "应该有多个失败原因");
    }

    private static PolicyCheckContext registerContext(String password, String username) {
        return PolicyCheckContext.builder()
                .password(password)
                .username(username)
                .scene(CredentialScene.REGISTER)
                .build();
    }

    private static PasswordValidator newValidator(PasswordPolicy... policies) {
        List<PasswordPolicy> list = List.of(policies);
        CredentialPolicyLoader loader = () -> list;
        return new DefaultPasswordValidator(loader);
    }
}
