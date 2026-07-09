package com.ingot.framework.security.crypto.jackson;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.utils.crypto.AESUtil;
import com.ingot.framework.security.crypto.annotation.InDecryptField;
import com.ingot.framework.security.crypto.annotation.InEncryptField;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.model.CryptoType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>信封加密字段级加解密往返测试。</p>
 *
 * <p>验证 {@link CryptoSerializer}/{@link CryptoDeserializer} 在 HYBRID 模式下复用请求上下文的
 * CEK/AAD 完成单字段加解密，以及无上下文拒绝与篡改抛出完整性异常。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class HybridFieldCryptoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] cek() {
        byte[] cek = new byte[32];
        new SecureRandom().nextBytes(cek);
        return cek;
    }

    private byte[] aad() {
        return "h1|test-2026|nonce1|1751760000000".getBytes(StandardCharsets.UTF_8);
    }

    private void bindContext(byte[] cek, byte[] aad) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HybridContext.ATTR_CEK, cek);
        request.setAttribute(HybridContext.ATTR_AAD, aad);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void fieldRoundTrip() throws Exception {
        bindContext(cek(), aad());

        EncDto enc = new EncDto();
        enc.id = "1";
        enc.secret = "13800000000";
        String json = objectMapper.writeValueAsString(enc);

        // 明文字段保持原样，敏感字段变为密文
        assertEquals("1", objectMapper.readTree(json).get("id").asText());
        assertNotEquals("13800000000", objectMapper.readTree(json).get("secret").asText());

        DecDto dec = objectMapper.readValue(json, DecDto.class);
        assertEquals("1", dec.id);
        assertEquals("13800000000", dec.secret);
    }

    @Test
    void missingContextFails() {
        EncDto enc = new EncDto();
        enc.id = "1";
        enc.secret = "plain-text";
        Exception ex = assertThrows(Exception.class, () -> objectMapper.writeValueAsString(enc));
        BizException biz = findBizException(ex);
        assertNotNull(biz);
        assertEquals(CryptoErrorCode.CRYPTO_HEADER_MISSING.getCode(), biz.getCode());
    }

    @Test
    void tamperedFieldFailsIntegrity() throws Exception {
        byte[] cek = cek();
        byte[] aad = aad();
        bindContext(cek, aad);

        // 用不同 AAD 生成的密文，解密时因 AAD 不一致触发完整性失败
        String badCipher = AESUtil.encryptGCM("13800000000".getBytes(StandardCharsets.UTF_8), cek,
                "h1|test-2026|tampered|1751760000000".getBytes(StandardCharsets.UTF_8));
        String json = "{\"id\":\"1\",\"secret\":\"" + badCipher + "\"}";

        // Jackson 会将反序列化期间抛出的 BizException 包装，业务异常存于根因链中
        Exception ex = assertThrows(Exception.class, () -> objectMapper.readValue(json, DecDto.class));
        BizException biz = findBizException(ex);
        assertNotNull(biz);
        assertEquals(CryptoErrorCode.CRYPTO_INTEGRITY_ERROR.getCode(), biz.getCode());
    }

    private BizException findBizException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof BizException biz) {
                return biz;
            }
            cause = cause.getCause();
        }
        return null;
    }

    static class EncDto {
        public String id;
        @InEncryptField(CryptoType.HYBRID)
        public String secret;
    }

    static class DecDto {
        public String id;
        @InDecryptField(CryptoType.HYBRID)
        public String secret;
    }
}
