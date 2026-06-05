package com.ingot.framework.gateway.rule.client.blacklist.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * CIDR 匹配工具：判断 IPv4/IPv6 是否在 CIDR 网段内。
 *
 * <p>实现简单、零依赖；只支持标准 CIDR 表达式如 {@code 192.168.1.0/24}、
 * {@code fe80::/10}。非法表达式或地址 IP 形态不匹配时返回 false。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public final class CidrMatcher {

    private CidrMatcher() {
    }

    public static boolean matches(String cidr, String ip) {
        if (cidr == null || ip == null) return false;
        int slash = cidr.indexOf('/');
        if (slash < 0) return false;
        String netStr = cidr.substring(0, slash);
        int prefix;
        try {
            prefix = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException e) {
            return false;
        }
        try {
            byte[] netBytes = InetAddress.getByName(netStr).getAddress();
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            if (netBytes.length != ipBytes.length) return false;
            int fullBytes = prefix / 8;
            int remainder = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (netBytes[i] != ipBytes[i]) return false;
            }
            if (remainder == 0) return true;
            int mask = 0xFF << (8 - remainder) & 0xFF;
            return (netBytes[fullBytes] & mask) == (ipBytes[fullBytes] & mask);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
