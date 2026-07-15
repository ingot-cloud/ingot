package com.ingot.framework.commons.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>客户端指纹工具类，支持前端设备指纹和服务端 IP+UA 两种模式</p>
 *
 * <p>推荐使用前端设备指纹模式：前端通过浏览器 API 计算设备指纹，
 * 通过自定义 Header（{@code In-Ca-Sig}）传给后端。该模式不受
 * Docker 部署、反向代理、IP 变化等因素影响，稳定性远优于 IP+UA。</p>
 *
 * <p>服务端 IP+UA 模式作为降级方案保留，适用于无法修改前端的场景。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 模式一（推荐）：直接使用前端传递的设备指纹
 * String deviceFp = request.getHeader(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
 * // deviceFp 即为指纹值，无需二次计算
 *
 * // 模式二（降级）：服务端计算 IP+UA 指纹
 * String fp = FingerprintUtil.compute(ip, ua);
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public final class FingerprintUtil {

    private FingerprintUtil() {
    }

    /**
     * 根据客户端 IP 和 User-Agent 计算指纹（服务端降级方案）
     *
     * @param ip        客户端 IP（可为 null）
     * @param userAgent User-Agent 字符串（可为 null）
     * @return SHA-256 十六进制摘要，计算异常时返回空字符串
     */
    public static String compute(String ip, String userAgent) {
        String normalizedIp = IpUtil.normalize(StrUtil.nullToDefault(ip, ""));
        String raw = normalizedIp + "|" + StrUtil.nullToDefault(userAgent, "");
        return sha256Hex(raw);
    }

    /**
     * 标准化 IP 地址：将 IPv6 loopback（{@code ::1} / {@code 0:0:0:0:0:0:0:1}）
     * 统一映射为 {@code 127.0.0.1}，确保 Servlet 和 WebFlux 环境计算结果一致。
     *
     * @deprecated 请使用 {@link IpUtil#normalize(String)}
     */
    @Deprecated(forRemoval = true)
    public static String normalizeIp(String ip) {
        return IpUtil.normalize(ip);
    }

    /**
     * 对任意原始字符串计算 SHA-256 十六进制摘要
     *
     * @param raw 原始字符串
     * @return SHA-256 十六进制摘要，计算异常时返回空字符串
     */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("[Fingerprint] compute error", e);
            return "";
        }
    }
}
