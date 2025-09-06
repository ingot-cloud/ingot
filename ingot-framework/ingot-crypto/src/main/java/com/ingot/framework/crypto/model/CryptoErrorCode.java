package com.ingot.framework.crypto.model;

import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : CryptoErrorCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:15 AM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum CryptoErrorCode implements ErrorCode {
    CRYPTO_CONFIG("crypto_config", "配置异常"),
    CRYPTO_TYPE_ERROR("crypto_type_error", "加密类型异常"),
    CRYPTO_SECRET_KEY("crypto_secret_key", "秘钥异常"),
    DECRYPT_ERROR("crypto_decrypt_error", "解密异常"),
    ENCRYPT_ERROR("crypto_encrypt_error", "加密异常");

    private final String code;
    private final String text;
}
