package com.ingot.framework.crypto.utils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.utils.crypto.AESUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.crypto.annotation.InDecrypt;
import com.ingot.framework.crypto.annotation.InEncrypt;
import com.ingot.framework.crypto.model.CryptoErrorCode;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.model.CryptoType;
import com.ingot.framework.crypto.web.SecretKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:02 AM.</p>
 */
@Slf4j
public class CryptoUtils {
    public static final String ENCRYPT_FAIL_CONTENT = "***";

    @Nullable
    public static CryptoInfoRecord getEncryptInfo(MethodParameter methodParameter) {
        InEncrypt encrypt = AnnotatedElementUtils.findMergedAnnotation(Objects.requireNonNull(methodParameter.getMethod()),
                InEncrypt.class);
        if (encrypt == null) {
            return null;
        }
        return new CryptoInfoRecord(encrypt.value(), encrypt.secretKey());
    }

    @Nullable
    public static CryptoInfoRecord getDecryptInfo(MethodParameter methodParameter) {
        InDecrypt decrypt = AnnotatedElementUtils.findMergedAnnotation(Objects.requireNonNull(methodParameter.getMethod()),
                InDecrypt.class);
        if (decrypt == null) {
            return null;
        }
        return new CryptoInfoRecord(decrypt.value(), decrypt.secretKey());
    }

    /**
     * 加密
     *
     * @param rawData 加密原数据
     * @param record  加密参数
     * @return 加密后的数据
     */
    public static String encrypt(byte[] rawData, CryptoInfoRecord record) {
        CryptoType type = record.type();
        if (type == null) {
            throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR);
        }

        String secretKey = getSecretKey(record);
        try {
            switch (type) {
                case AES -> {
                    return AESUtil.encryptCBC(rawData, secretKey);
                }
                case AES_GCM -> {
                    return AESUtil.encryptGCM(rawData, secretKey);
                }
                case RSA -> {
                    return SecureUtil.rsa(secretKey.getBytes(StandardCharsets.UTF_8), null)
                            .encryptBase64(rawData, KeyType.PrivateKey);
                }
            }
        } catch (Exception e) {
            throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR, e.getLocalizedMessage());
        }

        throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR);
        return "";
    }

    /**
     * 解密
     *
     * @param cryptoData 已加密的数据
     * @param record     加密参数
     * @return 明文数据
     */
    public static byte[] decrypt(byte[] cryptoData, CryptoInfoRecord record) {
        CryptoType type = record.type();
        if (type == null) {
            throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR);
        }

        String secretKey = getSecretKey(record);
        try {
            switch (type) {
                case AES -> {
                    return AESUtil.decryptCBC(cryptoData, secretKey).getBytes(StandardCharsets.UTF_8);
                }
                case AES_GCM -> {
                    return AESUtil.decryptGCM(cryptoData, secretKey).getBytes(StandardCharsets.UTF_8);
                }
                case RSA -> {
                    return SecureUtil.rsa(secretKey.getBytes(StandardCharsets.UTF_8), null)
                            .decrypt(cryptoData,
                                    KeyType.PrivateKey);
                }
            }
        } catch (Exception e) {
            throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR, e.getLocalizedMessage());
        }

        throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR);
        return cryptoData;
    }

    /**
     * AES解密
     */
    public static String decryptAES(String key, String crypto) {
        return StrUtil.str(
                decrypt(crypto.getBytes(StandardCharsets.UTF_8), new CryptoInfoRecord(CryptoType.AES, key)),
                StandardCharsets.UTF_8
        ).trim();
    }

    public static String getSecretKey(CryptoInfoRecord record) {
        String secretKey = record.secretKey();
        if (StrUtil.isEmpty(secretKey)) {
            secretKey = SpringContextHolder.getBean(SecretKeyResolver.class)
                    .get(record.type());
        }

        if (StrUtil.isEmpty(secretKey)) {
            log.error(logMsg("秘钥未配置 - 秘钥类型={}"), record.type().getText());
            throwError(CryptoErrorCode.CRYPTO_SECRET_KEY);
        }

        return secretKey;
    }

    public static void throwError(CryptoErrorCode code) {
        throw new BizException(code);
    }

    public static void throwError(CryptoErrorCode code, String message) {
        throw new BizException(code.getCode(), message);
    }

    public static String logMsg(String message) {
        return "[ingot-crypto] - " + message;
    }
}
