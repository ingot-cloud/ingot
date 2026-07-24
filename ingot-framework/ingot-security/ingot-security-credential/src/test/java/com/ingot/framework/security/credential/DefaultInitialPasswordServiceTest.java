package com.ingot.framework.security.credential;

import java.time.LocalDateTime;
import java.util.List;

import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy.Generation;
import com.ingot.framework.security.credential.model.InitialPasswordConfig;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.impl.DefaultInitialPasswordService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DefaultInitialPasswordService} 经 {@link CredentialPolicyLoader} 取生效配置的单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultInitialPasswordServiceTest {

    private DefaultInitialPasswordService service(InitialPasswordConfig config) {
        CredentialPolicyLoader loader = new CredentialPolicyLoader() {
            @Override
            public List<PasswordPolicy> loadPolicies() {
                return List.of();
            }

            @Override
            public InitialPasswordConfig getInitialPasswordConfig() {
                return config;
            }
        };
        return new DefaultInitialPasswordService(loader);
    }

    @Test
    void generate_random_matchesLengthAndHasUpperAndDigit() {
        String pwd = service(new InitialPasswordConfig(Generation.RANDOM, 12, "x", 72, true, true)).generate();

        assertEquals(12, pwd.length());
        assertTrue(pwd.chars().anyMatch(Character::isUpperCase), "应包含大写字母");
        assertTrue(pwd.chars().anyMatch(Character::isDigit), "应包含数字");
    }

    @Test
    void generate_fixed_returnsFixedPassword() {
        String pwd = service(new InitialPasswordConfig(Generation.FIXED, 10, "Fixed@2026", 72, true, true)).generate();

        assertEquals("Fixed@2026", pwd);
    }

    @Test
    void isExpired_validHoursZero_neverExpires() {
        assertFalse(service(new InitialPasswordConfig(Generation.RANDOM, 10, "x", 0, true, true))
                .isExpired(LocalDateTime.now().minusDays(365)));
    }

    @Test
    void isExpired_issuedAtNull_returnsFalse() {
        assertFalse(service(new InitialPasswordConfig(Generation.RANDOM, 10, "x", 72, true, true))
                .isExpired(null));
    }

    @Test
    void isExpired_beyondValidHours_returnsTrue() {
        assertTrue(service(new InitialPasswordConfig(Generation.RANDOM, 10, "x", 72, true, true))
                .isExpired(LocalDateTime.now().minusHours(100)));
    }

    @Test
    void isExpired_withinValidHours_returnsFalse() {
        assertFalse(service(new InitialPasswordConfig(Generation.RANDOM, 10, "x", 72, true, true))
                .isExpired(LocalDateTime.now().minusHours(1)));
    }

    @Test
    void isForceChangeOnFirstLogin_readsConfig() {
        assertFalse(service(new InitialPasswordConfig(Generation.RANDOM, 10, "x", 72, true, false))
                .isForceChangeOnFirstLogin());
        assertTrue(service(new InitialPasswordConfig(Generation.RANDOM, 10, "x", 72, true, true))
                .isForceChangeOnFirstLogin());
    }
}
