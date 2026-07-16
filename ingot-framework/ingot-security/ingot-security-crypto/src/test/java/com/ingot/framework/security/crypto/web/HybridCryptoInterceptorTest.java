package com.ingot.framework.security.crypto.web;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import cn.hutool.core.codec.Base64;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.utils.crypto.RSAUtil;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.hybrid.HybridHeaders;
import com.ingot.framework.security.crypto.hybrid.HybridKeyManager;
import com.ingot.framework.security.crypto.hybrid.HybridProtocolVersion;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>{@link HybridCryptoInterceptor} 协议头解析与 fail-close 测试。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class HybridCryptoInterceptorTest {

    private static final String KID = "test-2026";

    @InCryptoHybridContext
    static class StubController {

        @GetMapping("/hybrid")
        void endpoint() {
        }
    }

    private final HybridCryptoService hybridCryptoService = new HybridCryptoService();

    private HybridCryptoInterceptor interceptor(HybridKeyManager keyManager) {
        InCryptoProperties properties = new InCryptoProperties();
        properties.getHybrid().setActiveKid(KID);
        return new HybridCryptoInterceptor(properties, keyManager, hybridCryptoService, noReplayGuard());
    }

    private static ObjectProvider<com.ingot.framework.security.replay.ReplayGuard> noReplayGuard() {
        return new ObjectProvider<>() {
            @Override
            public com.ingot.framework.security.replay.ReplayGuard getObject() {
                return null;
            }

            @Override
            public com.ingot.framework.security.replay.ReplayGuard getObject(Object... args) {
                return null;
            }

            @Override
            public com.ingot.framework.security.replay.ReplayGuard getIfAvailable() {
                return null;
            }

            @Override
            public com.ingot.framework.security.replay.ReplayGuard getIfUnique() {
                return null;
            }
        };
    }

    private HybridKeyManager keyManager(KeyPair kp) {
        InCryptoProperties properties = new InCryptoProperties();
        InCryptoProperties.KeyPair pair = new InCryptoProperties.KeyPair();
        pair.setPublicKey(Base64.encode(kp.getPublic().getEncoded()));
        pair.setPrivateKey(Base64.encode(kp.getPrivate().getEncoded()));
        properties.getHybrid().setActiveKid(KID);
        properties.getHybrid().getKeyPairs().put(KID, pair);
        return new HybridKeyManager(properties);
    }

    private String wrapCek(PublicKey publicKey, byte[] cek) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec oaep = new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaep);
        return Base64.encode(cipher.doFinal(cek));
    }

    private HandlerMethod hybridHandler() throws NoSuchMethodException {
        return new HandlerMethod(new StubController(), StubController.class.getDeclaredMethod("endpoint"));
    }

    private void setRequiredHeaders(MockHttpServletRequest request, KeyPair kp, byte[] cek) throws Exception {
        request.addHeader(HybridHeaders.MODE, HybridProtocolVersion.H1.wireValue());
        request.addHeader(HybridHeaders.KID, KID);
        request.addHeader(HybridHeaders.WRAPPED_KEY, wrapCek(kp.getPublic(), cek));
        request.addHeader(HybridHeaders.NONCE, "nonce1");
        request.addHeader(HybridHeaders.TIMESTAMP, "1751760000000");
    }

    @Test
    void validHeadersEstablishContextAndEchoResponseHeaders() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridKeyManager manager = keyManager(kp);
        HybridCryptoInterceptor interceptor = interceptor(manager);
        HandlerMethod handler = hybridHandler();

        byte[] cek = new byte[32];
        new SecureRandom().nextBytes(cek);

        MockHttpServletRequest request = new MockHttpServletRequest();
        setRequiredHeaders(request, kp, cek);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, handler));

        byte[] storedCek = (byte[]) request.getAttribute(HybridContext.ATTR_CEK);
        byte[] storedAad = (byte[]) request.getAttribute(HybridContext.ATTR_AAD);
        assertNotNull(storedCek);
        assertArrayEquals(cek, storedCek);
        assertEquals("h1|test-2026|nonce1|1751760000000",
                new String(storedAad, StandardCharsets.UTF_8));

        assertEquals(HybridProtocolVersion.H1.wireValue(), response.getHeader(HybridHeaders.MODE));
        assertEquals(KID, response.getHeader(HybridHeaders.KID));
    }

    @Test
    void missingModeHeaderFailsClose() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridCryptoInterceptor interceptor = interceptor(keyManager(kp));
        HandlerMethod handler = hybridHandler();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HybridHeaders.KID, KID);
        request.addHeader(HybridHeaders.WRAPPED_KEY, wrapCek(kp.getPublic(), new byte[32]));
        MockHttpServletResponse response = new MockHttpServletResponse();

        BizException ex = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler));
        assertEquals(CryptoErrorCode.CRYPTO_HEADER_MISSING.getCode(), ex.getCode());
    }

    @Test
    void missingKidOrWrappedKeyFailsClose() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridCryptoInterceptor interceptor = interceptor(keyManager(kp));
        HandlerMethod handler = hybridHandler();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HybridHeaders.MODE, HybridProtocolVersion.H1.wireValue());
        MockHttpServletResponse response = new MockHttpServletResponse();

        BizException ex = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, handler));
        assertEquals(CryptoErrorCode.CRYPTO_HEADER_MISSING.getCode(), ex.getCode());
    }

    @Test
    void nonHybridHandlerIsSkipped() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridCryptoInterceptor interceptor = interceptor(keyManager(kp));

        HandlerMethod handler = new HandlerMethod(this, HybridCryptoInterceptorTest.class.getDeclaredMethod("validHeadersEstablishContextAndEchoResponseHeaders"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, handler));
        assertEquals(null, request.getAttribute(HybridContext.ATTR_CEK));
    }
}
