package com.ingot.framework.security.crypto.web;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.annotation.InDecrypt;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.model.CryptoType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>{@link InDecryptParamResolver} HYBRID URL 参数解密测试。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class InDecryptParamResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HybridCryptoService hybridCryptoService = new HybridCryptoService();
    private final InDecryptParamResolver resolver = new InDecryptParamResolver(
            new InCryptoProperties(), objectMapper, hybridCryptoService);

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
    void hybridParamRoundTrip() throws Exception {
        byte[] cek = cek();
        byte[] aad = aad();
        bindContext(cek, aad);

        String plain = "{\"name\":\"ingot\",\"page\":1}";
        String cipher = hybridCryptoService.encrypt(cek, plain.getBytes(StandardCharsets.UTF_8), aad);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/query");
        request.setParameter("data", cipher);
        request.setAttribute(HybridContext.ATTR_CEK, cek);
        request.setAttribute(HybridContext.ATTR_AAD, aad);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MethodParameter param = methodParam("hybridQuery");
        QueryDto result = (QueryDto) resolver.resolveArgument(
                param, null, new ServletWebRequest(request), null);

        assertNotNull(result);
        assertEquals("ingot", result.name);
        assertEquals(1, result.page);
    }

    @Test
    void missingContextFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/query");
        request.setParameter("data", "{\"name\":\"plain\",\"page\":2}");

        MethodParameter param = methodParam("hybridQuery");
        BizException ex = assertThrows(BizException.class, () ->
                resolver.resolveArgument(param, null, new ServletWebRequest(request), null));
        assertEquals(CryptoErrorCode.CRYPTO_HEADER_MISSING.getCode(), ex.getCode());
    }

    @Test
    void tamperedCipherFailsIntegrity() throws Exception {
        byte[] cek = cek();
        bindContext(cek, aad());

        String badCipher = hybridCryptoService.encrypt(
                cek, "{\"name\":\"x\"}".getBytes(StandardCharsets.UTF_8),
                "h1|test-2026|tampered|1751760000000".getBytes(StandardCharsets.UTF_8));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/query");
        request.setParameter("data", badCipher);
        request.setAttribute(HybridContext.ATTR_CEK, cek);
        request.setAttribute(HybridContext.ATTR_AAD, aad());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MethodParameter param = methodParam("hybridQuery");
        BizException ex = assertThrows(BizException.class, () ->
                resolver.resolveArgument(param, null, new ServletWebRequest(request), null));
        assertEquals(CryptoErrorCode.CRYPTO_INTEGRITY_ERROR.getCode(), ex.getCode());
    }

    private MethodParameter methodParam(String methodName) throws NoSuchMethodException {
        return new MethodParameter(StubController.class.getMethod(methodName, QueryDto.class), 0);
    }

    static class StubController {
        public void hybridQuery(@InDecrypt(CryptoType.HYBRID) QueryDto query) {
        }
    }

    static class QueryDto {
        public String name;
        public int page;
    }
}
