package com.ingot.framework.security.crypto.model;

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
    ENCRYPT_ERROR("crypto_encrypt_error", "加密异常"),
    CRYPTO_HEADER_MISSING("crypto_header_missing", "缺少加密协议头"),
    CRYPTO_KID_UNKNOWN("crypto_kid_unknown", "未知或已失效的密钥版本"),
    CRYPTO_KEY_UNWRAP_ERROR("crypto_key_unwrap_error", "内容密钥解包失败"),
    CRYPTO_INTEGRITY_ERROR("crypto_integrity_error", "数据完整性校验失败"),
    CRYPTO_ALG_UNSUPPORTED("crypto_alg_unsupported", "不支持的加密算法");

    private final String code;
    private final String text;
}
