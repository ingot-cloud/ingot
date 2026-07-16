package com.ingot.framework.security.crypto.hybrid;

import java.util.Optional;

/**
 * <p>HYBRID 信封加密协议版本标识。</p>
 *
 * <p>对应 {@link HybridHeaders#MODE} 头的传输值；后续协议升级（如 H2）在此扩展，
 * 拦截器可按版本分支处理算法与报文形态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum HybridProtocolVersion {

    /**
     * 信封加密 v1：RSA-OAEP-256 包裹 CEK + AES-256-GCM 内容加密
     */
    H1("h1");

    private final String wireValue;

    HybridProtocolVersion(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * 线上传输的协议版本字符串（写入 {@link HybridHeaders#MODE}）。
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * 解析已知协议版本；未知或空值返回 empty。
     */
    public static Optional<HybridProtocolVersion> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        for (HybridProtocolVersion version : values()) {
            if (version.wireValue.equals(value)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    /**
     * 当前服务端默认回写的协议版本。
     */
    public static HybridProtocolVersion current() {
        return H1;
    }
}
