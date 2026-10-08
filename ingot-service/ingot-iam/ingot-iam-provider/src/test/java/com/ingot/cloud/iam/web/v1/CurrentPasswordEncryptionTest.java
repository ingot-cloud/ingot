package com.ingot.cloud.iam.web.v1;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.account.CurrentAccountService;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.cloud.iam.persistence.SessionRepository;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.session.SessionService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CurrentPasswordInput;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.utils.crypto.RSAUtil;
import com.ingot.framework.core.error.GlobalExceptionHandlerResolver;
import com.ingot.framework.security.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.hybrid.HybridHeaders;
import com.ingot.framework.security.crypto.hybrid.HybridKeyManager;
import com.ingot.framework.security.crypto.hybrid.HybridProtocolVersion;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.web.HybridCryptoInterceptor;
import com.ingot.framework.security.crypto.web.InDecryptRequestBodyAdvice;
import com.ingot.framework.security.replay.ReplayGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>用实际 HYBRID 协议头、RSA 密钥包裹与 AES-GCM 报文验证密码 PUT 的解密及校验链路。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CurrentPasswordEncryptionTest {
    private static final String PATH = "/v1/me/password";
    private static final String KID = "password-test";
    private static final String PASSWORD = "test-new-password";
    private static final String OLD_PASSWORD = "test-old-password";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HybridCryptoService crypto = new HybridCryptoService();
    private final ChangePasswordUseCase passwords = mock(ChangePasswordUseCase.class);
    private final SessionService sessions = mock(SessionService.class);
    private final IamAccountEntity account = new IamAccountEntity();
    private KeyPair keyPair;
    private InCryptoProperties properties;
    private MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        keyPair = RSAUtil.generateKey();
        properties = new InCryptoProperties();
        properties.getHybrid().setActiveKid(KID);
        var pair = new InCryptoProperties.KeyPair();
        pair.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        pair.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        properties.getHybrid().getKeyPairs().put(KID, pair);
        var replay = mock(ReplayGuard.class);
        var provider = new StaticListableBeanFactory(Map.of("replayGuard", replay)).getBeanProvider(ReplayGuard.class);
        var interceptor = new HybridCryptoInterceptor(properties, new HybridKeyManager(properties), crypto, provider);
        var access = mock(IamAccess.class);
        var queries = mock(AccountQueryRepository.class);
        var context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2");
        when(access.requireCurrent()).thenReturn(new ActiveIdentity(context, "0", "0", null));
        account.setId(BigInteger.ONE);
        account.setMustChangePassword(true);
        when(queries.find(1L)).thenReturn(account);
        var service = new CurrentAccountService(access, queries, mock(AccountWriteRepository.class),
                mock(SessionRepository.class), passwords);
        mvc = MockMvcBuilders.standaloneSetup(new CurrentSessionAPI(sessions, service))
                .addInterceptors(interceptor)
                .setControllerAdvice(new InDecryptRequestBodyAdvice(properties, mapper, crypto),
                        new GlobalExceptionHandlerResolver())
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void wholeEncryptedPayloadReachesCorrectPasswordUseCase(boolean forced) throws Exception {
        account.setMustChangePassword(forced);
        var input = new CurrentPasswordInput(forced ? null : OLD_PASSWORD, PASSWORD, PASSWORD);
        mvc.perform(encryptedRequest(input, false)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        if (forced) {
            verify(passwords).forceChangePassword(argThat(command ->
                    command.getUserId().equals(1L) && PASSWORD.equals(command.getNewPassword())));
            verify(passwords, never()).changePassword(any());
        } else {
            verify(passwords).changePassword(argThat(command -> command.getUserId().equals(1L)
                    && OLD_PASSWORD.equals(command.getOldPassword()) && PASSWORD.equals(command.getNewPassword())
                    && PASSWORD.equals(command.getConfirmPassword())));
            verify(passwords, never()).forceChangePassword(any());
        }
        verifyNoInteractions(sessions);
    }

    @Test
    void decryptedBlankPasswordsStillFailValidation() throws Exception {
        mvc.perform(encryptedRequest(new CurrentPasswordInput(null, "", ""), false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseErrorCode.ILLEGAL_REQUEST_PARAMS.getCode()));
        verifyNoInteractions(passwords);
    }

    @Test
    void plaintextWithoutProtocolHeadersCannotChangePassword() throws Exception {
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(new CurrentPasswordInput(null, PASSWORD, PASSWORD))))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(CryptoErrorCode.CRYPTO_HEADER_MISSING.getCode()));
        verifyNoInteractions(passwords);
    }

    @Test
    void tamperedCiphertextFailsBeforePasswordUseCase() throws Exception {
        mvc.perform(encryptedRequest(new CurrentPasswordInput(null, PASSWORD, PASSWORD), true))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(CryptoErrorCode.CRYPTO_INTEGRITY_ERROR.getCode()));
        verifyNoInteractions(passwords);
    }

    private MockHttpServletRequestBuilder encryptedRequest(CurrentPasswordInput input, boolean tamper) throws Exception {
        var cek = new byte[32];
        new SecureRandom().nextBytes(cek);
        String nonce = UUID.randomUUID().toString();
        String timestamp = Long.toString(System.currentTimeMillis());
        String mode = HybridProtocolVersion.current().wireValue();
        var aad = HybridContext.buildAad(mode, KID, nonce, timestamp);
        String ciphertext = crypto.encrypt(cek, mapper.writeValueAsBytes(input), aad);
        if (tamper) {
            var bytes = Base64.getDecoder().decode(ciphertext);
            bytes[bytes.length - 1] ^= 1;
            ciphertext = Base64.getEncoder().encodeToString(bytes);
        }
        var wrapper = Cipher.getInstance("RSA/ECB/OAEPPadding");
        wrapper.init(Cipher.ENCRYPT_MODE, keyPair.getPublic(), new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        return put(PATH).contentType(MediaType.APPLICATION_JSON)
                .header(HybridHeaders.MODE, mode).header(HybridHeaders.KID, KID)
                .header(HybridHeaders.WRAPPED_KEY, Base64.getEncoder().encodeToString(wrapper.doFinal(cek)))
                .header(HybridHeaders.NONCE, nonce).header(HybridHeaders.TIMESTAMP, timestamp)
                .content(mapper.writeValueAsBytes(Map.of(properties.getBodyKey(), ciphertext)));
    }
}
