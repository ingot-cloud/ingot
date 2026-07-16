package com.ingot.framework.security.crypto.hybrid;

/**
 * <p>HYBRID 信封加密 HTTP 协议头名称常量。</p>
 *
 * <p>头名称作为 wire protocol 固定约定，与平台 {@code In-*} 自定义头命名一致，不可通过配置重命名。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see HybridProtocolVersion
 */
public final class HybridHeaders {

    private HybridHeaders() {
    }

    /**
     * 模式/协议版本头（值见 {@link HybridProtocolVersion}）
     */
    public static final String MODE = "In-Crypto-Md";

    /**
     * 公钥版本 kid
     */
    public static final String KID = "In-Crypto-Kv";

    /**
     * RSA-OAEP 包裹的 CEK（Base64）
     */
    public static final String WRAPPED_KEY = "In-Crypto-Sk";

    /**
     * 防重放随机数
     */
    public static final String NONCE = "In-Crypto-No";

    /**
     * 防重放毫秒时间戳
     */
    public static final String TIMESTAMP = "In-Crypto-Ts";

    /**
     * 密钥包裹算法协商（可选，缺省 RSA-OAEP-256）
     */
    public static final String KEY_ALG = "In-Crypto-Al";

    /**
     * 内容加密算法协商（可选，缺省 A256GCM）
     */
    public static final String CONTENT_ENC = "In-Crypto-En";
}
