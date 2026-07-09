package com.ingot.framework.security.crypto.hybrid;

import com.ingot.framework.commons.utils.crypto.AESUtil;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.utils.CryptoUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>信封加密内容加解密服务。</p>
 *
 * <p>用内容密钥 CEK 对业务数据做 AES-256-GCM 加解密，并以 AAD 绑定协议头防篡改；
 * CEK 的解包由 {@link HybridKeyManager} 负责。本期仅支持默认套件 {@link #DEFAULT_ALG} 与 {@link #DEFAULT_ENC}。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see HybridKeyManager
 */
@Slf4j
public class HybridCryptoService {

    /**
     * 默认密钥包裹算法
     */
    public static final String DEFAULT_ALG = "RSA-OAEP-256";
    /**
     * 默认内容加密算法
     */
    public static final String DEFAULT_ENC = "A256GCM";

    /**
     * 校验算法协商是否受支持，本期仅支持默认套件。
     *
     * @param alg 密钥包裹算法，可为空（表示默认）
     * @param enc 内容加密算法，可为空（表示默认）
     */
    public void checkAlgorithm(String alg, String enc) {
        if (alg != null && !DEFAULT_ALG.equalsIgnoreCase(alg)) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_ALG_UNSUPPORTED);
        }
        if (enc != null && !DEFAULT_ENC.equalsIgnoreCase(enc)) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_ALG_UNSUPPORTED);
        }
    }

    /**
     * 用 CEK 解密业务数据并校验完整性。
     *
     * @param cek              内容密钥原始字节
     * @param cipherBase64Bytes Base64(iv‖密文‖tag) 的字节形式
     * @param aad              附加认证数据
     * @return 明文字节
     */
    public byte[] decrypt(byte[] cek, byte[] cipherBase64Bytes, byte[] aad) {
        try {
            return AESUtil.decryptGCM(cipherBase64Bytes, cek, aad);
        } catch (Exception e) {
            log.error(CryptoUtils.logMsg("HYBRID 解密/完整性校验失败"), e);
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_INTEGRITY_ERROR);
            return null;
        }
    }

    /**
     * 用 CEK 加密业务数据。
     *
     * @param cek   内容密钥原始字节
     * @param plain 明文字节
     * @param aad   附加认证数据
     * @return Base64(iv‖密文‖tag)
     */
    public String encrypt(byte[] cek, byte[] plain, byte[] aad) {
        try {
            return AESUtil.encryptGCM(plain, cek, aad);
        } catch (Exception e) {
            log.error(CryptoUtils.logMsg("HYBRID 加密失败"), e);
            CryptoUtils.throwError(CryptoErrorCode.ENCRYPT_ERROR);
            return null;
        }
    }
}
