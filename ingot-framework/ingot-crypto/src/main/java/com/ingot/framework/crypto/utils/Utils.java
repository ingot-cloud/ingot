package com.ingot.framework.crypto.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.symmetric.AES;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.error.exception.BizException;
import com.ingot.framework.core.utils.WebUtils;
import com.ingot.framework.crypto.annotation.IngotDecrypt;
import com.ingot.framework.crypto.annotation.IngotEncrypt;
import com.ingot.framework.crypto.model.CryptoErrorCode;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.model.CryptoType;
import com.ingot.framework.crypto.web.SecretKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:02 AM.</p>
 */
@Slf4j
public class Utils {

    @Nullable
    public static CryptoInfoRecord getEncryptInfo(MethodParameter methodParameter) {
        IngotEncrypt encrypt = AnnotatedElementUtils.findMergedAnnotation(Objects.requireNonNull(methodParameter.getMethod()),
                IngotEncrypt.class);
        if (encrypt == null) {
            return null;
        }
        return new CryptoInfoRecord(encrypt.value(), encrypt.secretKey());
    }

    @Nullable
    public static CryptoInfoRecord getDecryptInfo(MethodParameter methodParameter) {
        IngotDecrypt decrypt = AnnotatedElementUtils.findMergedAnnotation(Objects.requireNonNull(methodParameter.getMethod()),
                IngotDecrypt.class);
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
        switch (type) {
            case AES -> {
                AES aes = new AES(Mode.CBC, Padding.NoPadding, new SecretKeySpec(secretKey.getBytes(), "AES"),
                        new IvParameterSpec(secretKey.getBytes()));
                return aes.encryptBase64(rawData);
            }
            case RSA -> {
                return SecureUtil.rsa(secretKey.getBytes(StandardCharsets.UTF_8), null)
                        .encryptBase64(rawData, KeyType.PrivateKey);
            }
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
        switch (type) {
            case AES -> {
                AES aes = new AES(Mode.CBC, Padding.NoPadding,
                        new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES"),
                        new IvParameterSpec(secretKey.getBytes(StandardCharsets.UTF_8)));
                return aes.decrypt(StrUtil.str(cryptoData, StandardCharsets.UTF_8));
            }
            case RSA -> {
                return SecureUtil.rsa(secretKey.getBytes(StandardCharsets.UTF_8), null)
                        .decrypt(cryptoData,
                                KeyType.PrivateKey);
            }
        }
        throwError(CryptoErrorCode.CRYPTO_TYPE_ERROR);
        return cryptoData;
    }

    public static String getSecretKey(CryptoInfoRecord record) {
        String secretKey = record.secretKey();
        if (StrUtil.isEmpty(secretKey)) {
            secretKey = SpringContextHolder.getBean(SecretKeyResolver.class)
                    .get(WebUtils.getRequest(), record.type());
        }

        if (StrUtil.isEmpty(secretKey)) {
            log.error(logMsg("秘钥未配置 - 秘钥类型={}"), record.secretKey());
            throwError(CryptoErrorCode.CRYPTO_SECRET_KEY);
        }

        return secretKey;
    }

    public static void throwError(CryptoErrorCode code) {
        throw new BizException(code);
    }

    public static String logMsg(String message) {
        return "[ingot-crypto] - " + message;
    }
}
