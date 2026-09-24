package com.ingot.cloud.iam.web.v1.platform;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.iam.ApplicationPurgeInput;
import com.ingot.framework.commons.model.iam.SensitiveConfirmationKind;
import com.ingot.framework.commons.utils.crypto.AESUtil;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 强制清除入站 {@code secret} 必须先解字段密文，才能交给口令确认。
 *
 * @author jy
 * @since 1.0.0
 */
class ApplicationPurgeRequestDecryptTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void decryptsFieldEncryptedSecret() throws Exception {
        byte[] cek = new byte[32];
        new SecureRandom().nextBytes(cek);
        byte[] aad = "h1|test-2026|nonce1|1751760000000".getBytes(StandardCharsets.UTF_8);
        bindContext(cek, aad);

        String cipher = AESUtil.encryptGCM("correct-password".getBytes(StandardCharsets.UTF_8), cek, aad);
        assertNotEquals("correct-password", cipher);

        ApplicationPurgeRequest request = objectMapper.readValue("""
                {"expectedVersion":"1","confirmation":{"kind":"LOGIN_PASSWORD","secret":"%s"}}
                """.formatted(cipher), ApplicationPurgeRequest.class);

        assertEquals("1", request.getExpectedVersion());
        assertEquals(SensitiveConfirmationKind.LOGIN_PASSWORD, request.getConfirmation().getKind());
        assertEquals("correct-password", request.getConfirmation().getSecret());
        assertEquals("correct-password", request.toInput().confirmation().secret());
    }

    @Test
    void commonsInputKeepsCiphertextWithoutDecryptField() throws Exception {
        byte[] cek = new byte[32];
        new SecureRandom().nextBytes(cek);
        byte[] aad = "h1|test-2026|nonce1|1751760000000".getBytes(StandardCharsets.UTF_8);
        bindContext(cek, aad);

        String cipher = AESUtil.encryptGCM("correct-password".getBytes(StandardCharsets.UTF_8), cek, aad);
        ApplicationPurgeInput input = objectMapper.readValue("""
                {"expectedVersion":"1","confirmation":{"kind":"LOGIN_PASSWORD","secret":"%s"}}
                """.formatted(cipher), ApplicationPurgeInput.class);
        assertEquals(cipher, input.confirmation().secret());
    }

    private static void bindContext(byte[] cek, byte[] aad) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HybridContext.ATTR_CEK, cek);
        request.setAttribute(HybridContext.ATTR_AAD, aad);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
