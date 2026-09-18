package com.ingot.framework.security.config.annotation.web.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>无 {@code {id}} 前缀的历史 BCrypt 哈希可被默认匹配算法校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class PasswordEncoderConfigurationTest {
    private static final String RAW = "password";
    private static final String BARE_BCRYPT =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    @Test
    void matchesBareBcryptAndPrefixedHash() {
        PasswordEncoder encoder = PasswordEncoderConfiguration.createDelegatingPasswordEncoder();
        assertTrue(encoder.matches(RAW, BARE_BCRYPT));
        String encoded = encoder.encode(RAW);
        assertTrue(encoded.startsWith("{bcrypt}"));
        assertTrue(encoder.matches(RAW, encoded));
        assertFalse(encoder.matches("wrong", BARE_BCRYPT));
    }
}
