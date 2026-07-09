package com.ingot.framework.security.crypto.hybrid;

import java.nio.charset.StandardCharsets;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * <p>信封加密请求上下文常量与工具。</p>
 *
 * <p>定义请求处理期间在 request attribute 中传递 CEK 与 AAD 的键，以及 AAD 的规范化拼接方式，
 * 供拦截器、请求体解密与响应体加密三处共享同一份上下文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class HybridContext {

    private HybridContext() {
    }

    /**
     * 内容密钥 CEK（byte[]）在 request attribute 中的键
     */
    public static final String ATTR_CEK = "ingot.crypto.hybrid.cek";
    /**
     * 附加认证数据 AAD（byte[]）在 request attribute 中的键
     */
    public static final String ATTR_AAD = "ingot.crypto.hybrid.aad";
    /**
     * 标记本请求已按 HYBRID 解密、响应需加密
     */
    public static final String ATTR_ACTIVE = "ingot.crypto.hybrid.active";

    /**
     * 规范化拼接 AAD：{@code mode|kid|nonce|ts}。
     *
     * @param mode  模式标记头值
     * @param kid   密钥版本头值
     * @param nonce 随机数头值
     * @param ts    时间戳头值
     * @return AAD 字节
     */
    public static byte[] buildAad(String mode, String kid, String nonce, String ts) {
        String raw = String.join("|",
                nullToEmpty(mode), nullToEmpty(kid), nullToEmpty(nonce), nullToEmpty(ts));
        return raw.getBytes(StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    /**
     * 读取当前请求上下文中的内容密钥 CEK。
     *
     * @return CEK 字节；无 Web 上下文或未建立信封加密上下文时返回 {@code null}
     */
    public static byte[] currentCek() {
        return currentAttribute(ATTR_CEK);
    }

    /**
     * 读取当前请求上下文中的附加认证数据 AAD。
     *
     * @return AAD 字节；无 Web 上下文或未建立信封加密上下文时返回 {@code null}
     */
    public static byte[] currentAad() {
        return currentAttribute(ATTR_AAD);
    }

    private static byte[] currentAttribute(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
        return value instanceof byte[] bytes ? bytes : null;
    }
}
