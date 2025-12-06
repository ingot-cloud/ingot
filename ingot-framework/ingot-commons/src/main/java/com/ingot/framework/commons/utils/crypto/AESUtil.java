package com.ingot.framework.commons.utils.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import cn.hutool.core.codec.Base64;
import jakarta.validation.constraints.NotNull;

/**
 * <p>Description  : AESUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/26.</p>
 * <p>Time         : 11:05.</p>
 */
public final class AESUtil {

    private static final String AES = "AES";
    private static final int IV_LENGTH = 16; // CBC: 16字节
    private static final int GCM_IV_LENGTH = 12; // GCM推荐12字节
    private static final int GCM_TAG_LENGTH = 128; // 128位认证标签

    private static final String CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    private static final String GCM_NOPADDING = "AES/GCM/NoPadding";

    private static final SecureRandom random = new SecureRandom();

    // =================== CBC ===================

    /**
     * CBC模式加密，PKCS5Padding
     *
     * @param plainText 带加密数据
     * @param key       秘钥
     * @return Base64(IV[16字节] + Ciphertext)
     * @throws Exception 异常
     */
    public static String encryptCBC(String plainText, String key) throws Exception {
        return encryptCBC(plainText.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * CBC模式加密，PKCS5Padding
     *
     * @param plainText 带加密数据
     * @param key       秘钥
     * @return Base64(IV[16字节] + Ciphertext)
     * @throws Exception 异常
     */
    public static String encryptCBC(byte[] plainText, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CBC_PKCS5PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, AES),
                new IvParameterSpec(iv));

        return encrypt(plainText, cipher, iv);
    }

    /**
     * CBC模式解密，PKCS5Padding
     *
     * @param cipherText 密文数据Base64(IV[16字节] + Ciphertext)
     * @param key        秘钥
     * @return 明文
     * @throws Exception 异常
     */
    public static String decryptCBC(String cipherText, String key) throws Exception {
        return decryptCBC(key, Base64.decode(cipherText));
    }

    /**
     * CBC模式解密，PKCS5Padding
     *
     * @param cipherText 密文数据Base64(IV[16字节] + Ciphertext)
     * @param key        秘钥
     * @return 明文
     * @throws Exception 异常
     */
    public static String decryptCBC(byte[] cipherText, String key) throws Exception {
        return decryptCBC(key, Base64.decode(cipherText));
    }

    @NotNull
    private static String decryptCBC(String key, byte[] combined) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(CBC_PKCS5PADDING);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES),
                new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    // =================== GCM ===================

    /**
     * GCM模式加密，NoPadding，流模式它内部把 AES-ECB 当作一个伪随机数生成器，
     * 然后对明文逐字节做异或所以 GCM 不要求输入必须是 16 字节倍数，使用NoPadding
     *
     * @param plainText 带加密数据
     * @param key       秘钥
     * @return 密文
     * @throws Exception 异常
     */
    public static String encryptGCM(String plainText, String key) throws Exception {
        return encryptGCM(plainText.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * GCM模式加密，NoPadding，流模式它内部把 AES-ECB 当作一个伪随机数生成器，
     * 然后对明文逐字节做异或所以 GCM 不要求输入必须是 16 字节倍数，使用NoPadding
     *
     * @param plainText 带加密数据
     * @param key       秘钥
     * @return 密文
     * @throws Exception 异常
     */
    public static String encryptGCM(byte[] plainText, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(GCM_NOPADDING);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, AES), spec);

        return encrypt(plainText, cipher, iv);
    }

    /**
     * CBC模式解密，NoPadding
     *
     * @param cipherText 密文数据
     * @param key        秘钥
     * @return 明文
     * @throws Exception 异常
     */
    public static String decryptGCM(String cipherText, String key) throws Exception {
        return decryptGCM(key, Base64.decode(cipherText));
    }

    /**
     * CBC模式解密，NoPadding
     *
     * @param cipherText 密文数据
     * @param key        秘钥
     * @return 明文
     * @throws Exception 异常
     */
    public static String decryptGCM(byte[] cipherText, String key) throws Exception {
        return decryptGCM(key, Base64.decode(cipherText));
    }

    private static String encrypt(byte[] plainText, Cipher cipher, byte[] iv) throws Exception {
        byte[] encrypted = cipher.doFinal(plainText);

        // 拼接 iv + 密文 + tag
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.encode(combined);
    }

    @NotNull
    private static String decryptGCM(String keyStr, byte[] combined) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

        SecretKeySpec keySpec = new SecretKeySpec(keyStr.getBytes(StandardCharsets.UTF_8), AES);

        Cipher cipher = Cipher.getInstance(GCM_NOPADDING);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

        byte[] decrypted = cipher.doFinal(cipherBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 生成随机主密钥（用于 JWK 密钥加密）
     * 
     * @return Base64 编码的随机密钥（256 位）
     */
    public static String generateMasterKey() {
        byte[] key = new byte[32];  // 256 位
        random.nextBytes(key);
        return Base64.encode(key);
    }

}
