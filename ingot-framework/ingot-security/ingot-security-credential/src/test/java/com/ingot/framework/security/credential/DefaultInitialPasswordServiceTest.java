package com.ingot.framework.security.credential;

import java.time.LocalDateTime;

import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy;
import com.ingot.framework.security.credential.service.impl.DefaultInitialPasswordService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DefaultInitialPasswordService} 初始密码生成与有效期判定单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultInitialPasswordServiceTest {

    private final CredentialSecurityProperties properties = new CredentialSecurityProperties();

    private DefaultInitialPasswordService service() {
        return new DefaultInitialPasswordService(properties);
    }

    private InitialPasswordPolicy policy() {
        return properties.getPolicy().getInitialPassword();
    }

    @Test
    void generate_random_matchesLengthAndHasUpperAndDigit() {
        policy().setGeneration(InitialPasswordPolicy.Generation.RANDOM);
        policy().setLength(12);

        String pwd = service().generate();

        assertEquals(12, pwd.length());
        assertTrue(pwd.chars().anyMatch(Character::isUpperCase), "应包含大写字母");
        assertTrue(pwd.chars().anyMatch(Character::isDigit), "应包含数字");
    }

    @Test
    void generate_fixed_returnsFixedPassword() {
        policy().setGeneration(InitialPasswordPolicy.Generation.FIXED);
        policy().setFixedPassword("Fixed@2026");

        assertEquals("Fixed@2026", service().generate());
    }

    @Test
    void isExpired_validHoursZero_neverExpires() {
        policy().setValidHours(0);

        assertFalse(service().isExpired(LocalDateTime.now().minusDays(365)));
    }

    @Test
    void isExpired_issuedAtNull_returnsFalse() {
        policy().setValidHours(72);

        assertFalse(service().isExpired(null));
    }

    @Test
    void isExpired_beyondValidHours_returnsTrue() {
        policy().setValidHours(72);

        assertTrue(service().isExpired(LocalDateTime.now().minusHours(100)));
    }

    @Test
    void isExpired_withinValidHours_returnsFalse() {
        policy().setValidHours(72);

        assertFalse(service().isExpired(LocalDateTime.now().minusHours(1)));
    }

    @Test
    void isForceChangeOnFirstLogin_readsConfig() {
        policy().setForceChangeOnFirstLogin(false);
        assertFalse(service().isForceChangeOnFirstLogin());

        policy().setForceChangeOnFirstLogin(true);
        assertTrue(service().isForceChangeOnFirstLogin());
    }
}
