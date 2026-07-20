package com.ingot.framework.security.crypto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : 加密属性.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:30 AM.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.crypto")
public class InCryptoProperties {
    /**
     * url参数加解密key
     */
    private String paramKey = "data";
    /**
     * body参数加解密key
     */
    private String bodyKey = "data";
    /**
     * 秘钥
     */
    private Map<String, String> secretKeys;
    /**
     * 信封加密（HYBRID）配置
     */
    private Hybrid hybrid = new Hybrid();

    /**
     * <p>响应体的加密包装方式。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    public enum ResponseWrap {
        /**
         * 保留 R 结构，仅加密 data
         */
        DATA_ONLY,
        /**
         * 整个响应体整体加密
         */
        FULL
    }

    /**
     * <p>信封加密（HYBRID）的配置项集合。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Getter
    @Setter
    public static class Hybrid {
        /**
         * 响应包装方式
         */
        private ResponseWrap responseWrap = ResponseWrap.DATA_ONLY;
        /**
         * 当前活跃密钥版本
         */
        private String activeKid;
        /**
         * 多版本密钥对，key 为 kid
         */
        private Map<String, KeyPair> keyPairs = new LinkedHashMap<>();
        /**
         * 是否暴露公钥下发端点
         */
        private boolean publicKeyEndpointEnabled = true;
        /**
         * 防重放命名空间
         */
        private String replayNamespace = "crypto";
    }

    /**
     * <p>Base64 编码的非对称密钥对（X509 公钥 / PKCS8 私钥）。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Getter
    @Setter
    public static class KeyPair {
        /**
         * 公钥，X509 Base64
         */
        private String publicKey;
        /**
         * 私钥，PKCS8 Base64
         */
        private String privateKey;
    }
}
