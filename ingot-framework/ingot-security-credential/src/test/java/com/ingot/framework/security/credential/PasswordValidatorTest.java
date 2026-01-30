package com.ingot.framework.security.credential;

import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import com.ingot.framework.security.credential.policy.PasswordStrengthPolicy;
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

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        // 创建默认策略
        PasswordStrengthPolicy strengthPolicy = new PasswordStrengthPolicy();
        strengthPolicy.setMinLength(8);
        strengthPolicy.setRequireUppercase(true);
        strengthPolicy.setRequireLowercase(true);
        strengthPolicy.setRequireDigit(true);
        strengthPolicy.setRequireSpecialChar(false);
        
//        validator = new DefaultPasswordValidator(Collections.singletonList(strengthPolicy));
    }

    @Test
    void testValidPassword() {
        // 测试有效密码
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("Test1234")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertTrue(result.isPassed(), "密码应该通过校验");
        assertTrue(result.getFailureReasons().isEmpty(), "不应该有失败原因");
    }

    @Test
    void testPasswordTooShort() {
        // 测试密码过短
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("Test1")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("密码长度不足"), "应该包含长度不足的错误");
    }

    @Test
    void testPasswordNoUppercase() {
        // 测试缺少大写字母
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("test1234")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("大写字母"), "应该包含缺少大写字母的错误");
    }

    @Test
    void testPasswordNoLowercase() {
        // 测试缺少小写字母
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("TEST1234")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("小写字母"), "应该包含缺少小写字母的错误");
    }

    @Test
    void testPasswordNoDigit() {
        // 测试缺少数字
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("TestTest")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("数字"), "应该包含缺少数字的错误");
    }

    @Test
    void testPasswordContainsUsername() {
        // 测试包含用户名
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("Testuser1234")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("用户名"), "应该包含不能包含用户名的错误");
    }

    @Test
    void testPasswordWithForbiddenPattern() {
        // 测试禁止模式
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("Password123")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureMessage().contains("禁止使用的常见模式"), "应该包含禁止模式的错误");
    }

    @Test
    void testMultipleErrors() {
        // 测试多个错误
        PolicyCheckContext context = PolicyCheckContext.builder()
                .password("test")
                .username("testuser")
                .build();
        
        PasswordCheckResult result = validator.validate(context);
        
        assertFalse(result.isPassed(), "密码应该校验失败");
        assertTrue(result.getFailureReasons().size() > 1, "应该有多个失败原因");
    }
}
