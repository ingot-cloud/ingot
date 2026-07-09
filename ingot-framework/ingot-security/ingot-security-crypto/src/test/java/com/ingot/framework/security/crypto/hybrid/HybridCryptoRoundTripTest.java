package com.ingot.framework.security.crypto.hybrid;

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
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 信封加密核心往返测试：RSA-OAEP 包裹/解包 CEK + AES-GCM(AAD) 收发。
 *
 * @author ingot
 */
class HybridCryptoRoundTripTest {

    private static final String KID = "test-2026";

    private HybridKeyManager keyManager(KeyPair kp) throws Exception {
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

    @Test
    void fullRoundTrip() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridKeyManager manager = keyManager(kp);
        HybridCryptoService service = new HybridCryptoService();

        byte[] cek = new byte[32];
        new SecureRandom().nextBytes(cek);
        String wrapped = wrapCek(kp.getPublic(), cek);

        byte[] unwrapped = manager.unwrapCek(KID, wrapped);
        assertArrayEquals(cek, unwrapped);

        byte[] aad = "h1|test-2026|nonce1|1751760000000".getBytes(StandardCharsets.UTF_8);
        String plain = "{\"name\":\"ingot\",\"amount\":100}";
        String cipher = service.encrypt(unwrapped, plain.getBytes(StandardCharsets.UTF_8), aad);

        byte[] decrypted = service.decrypt(unwrapped, cipher.getBytes(StandardCharsets.UTF_8), aad);
        assertEquals(plain, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    void tamperedAadFailsIntegrity() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridKeyManager manager = keyManager(kp);
        HybridCryptoService service = new HybridCryptoService();

        byte[] cek = new byte[32];
        new SecureRandom().nextBytes(cek);
        byte[] unwrapped = manager.unwrapCek(KID, wrapCek(kp.getPublic(), cek));

        byte[] aad = "h1|test-2026|nonce1|1751760000000".getBytes(StandardCharsets.UTF_8);
        String cipher = service.encrypt(unwrapped, "data".getBytes(StandardCharsets.UTF_8), aad);

        byte[] tamperedAad = "h1|test-2026|nonceX|1751760000000".getBytes(StandardCharsets.UTF_8);
        BizException ex = assertThrows(BizException.class,
                () -> service.decrypt(unwrapped, cipher.getBytes(StandardCharsets.UTF_8), tamperedAad));
        assertEquals(CryptoErrorCode.CRYPTO_INTEGRITY_ERROR.getCode(), ex.getCode());
    }

    @Test
    void unknownKidRejected() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridKeyManager manager = keyManager(kp);
        BizException ex = assertThrows(BizException.class,
                () -> manager.unwrapCek("no-such-kid", "AAAA"));
        assertEquals(CryptoErrorCode.CRYPTO_KID_UNKNOWN.getCode(), ex.getCode());
    }

    @Test
    void publicKeyInfoExposesActiveKid() throws Exception {
        KeyPair kp = RSAUtil.generateKey();
        HybridKeyManager manager = keyManager(kp);
        var infos = manager.publicKeyInfos();
        assertEquals(1, infos.size());
        assertEquals(KID, infos.get(0).kid());
        assertEquals(HybridCryptoService.DEFAULT_ALG, infos.get(0).alg());
        assertEquals(true, infos.get(0).active());
    }

    @Test
    void refreshReloadsRotatedKeys() throws Exception {
        KeyPair oldKp = RSAUtil.generateKey();
        InCryptoProperties properties = new InCryptoProperties();
        InCryptoProperties.KeyPair oldPair = new InCryptoProperties.KeyPair();
        oldPair.setPublicKey(Base64.encode(oldKp.getPublic().getEncoded()));
        oldPair.setPrivateKey(Base64.encode(oldKp.getPrivate().getEncoded()));
        properties.getHybrid().setActiveKid(KID);
        properties.getHybrid().getKeyPairs().put(KID, oldPair);

        HybridKeyManager manager = new HybridKeyManager(properties);
        assertEquals(KID, manager.publicKeyInfos().get(0).kid());

        // 模拟配置刷新：轮换到新 kid
        String newKid = "test-2027";
        KeyPair newKp = RSAUtil.generateKey();
        InCryptoProperties.KeyPair newPair = new InCryptoProperties.KeyPair();
        newPair.setPublicKey(Base64.encode(newKp.getPublic().getEncoded()));
        newPair.setPrivateKey(Base64.encode(newKp.getPrivate().getEncoded()));
        properties.getHybrid().getKeyPairs().clear();
        properties.getHybrid().getKeyPairs().put(newKid, newPair);
        properties.getHybrid().setActiveKid(newKid);

        manager.refresh();

        var infos = manager.publicKeyInfos();
        assertEquals(1, infos.size());
        assertEquals(newKid, infos.get(0).kid());
        assertEquals(true, infos.get(0).active());

        // 旧 kid 刷新后不可用
        BizException ex = assertThrows(BizException.class, () -> manager.unwrapCek(KID, "AAAA"));
        assertEquals(CryptoErrorCode.CRYPTO_KID_UNKNOWN.getCode(), ex.getCode());
    }
}
