package com.ingot.framework.security.crypto.hybrid;

/**
 * <p>公钥下发信息，用于 {@code GET /crypto/public-keys} 端点向前端下发可用公钥及其版本。</p>
 *
 * @param kid       密钥版本
 * @param alg       密钥包裹算法
 * @param publicKey 公钥（X509 Base64）
 * @param active    是否为当前活跃密钥
 * @author jy
 * @since 1.0.0
 */
public record PublicKeyInfo(String kid, String alg, String publicKey, boolean active) {
}
