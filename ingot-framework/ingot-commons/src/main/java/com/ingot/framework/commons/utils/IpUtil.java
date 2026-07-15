package com.ingot.framework.commons.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * <p>IP 字符串标准化工具。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IpUtil {

    private IpUtil() {
    }

    /**
     * 标准化 IP 地址：将 IPv6 loopback（{@code ::1} / {@code 0:0:0:0:0:0:0:1}）
     * 统一映射为 {@code 127.0.0.1}，确保 Servlet 和 WebFlux 环境结果一致。
     *
     * @param ip 原始 IP 字符串
     * @return 标准化后的 IP
     */
    public static String normalize(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    /**
     * 从 socket 远端地址解析 IP 字符串。
     *
     * <p>WebFlux/Netty 下 {@link InetSocketAddress#getAddress()} 可能为 null（未解析），
     * 此时回退 {@link InetSocketAddress#getHostString()}。</p>
     *
     * @param address 远端地址
     * @return IP 或主机名字符串，address 为 null 时返回 null
     */
    public static String fromInetSocketAddress(InetSocketAddress address) {
        if (address == null) {
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        if (inetAddress != null) {
            return inetAddress.getHostAddress();
        }
        return address.getHostString();
    }
}
