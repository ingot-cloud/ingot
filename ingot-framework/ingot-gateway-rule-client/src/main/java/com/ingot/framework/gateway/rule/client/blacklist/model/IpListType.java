package com.ingot.framework.gateway.rule.client.blacklist.model;

import java.util.Locale;

/**
 * 名单类型：黑名单 / 白名单。
 *
 * <p>DB 表 {@code gateway_ip_list.list_type} 为 {@code char(1)}：{@code B} / {@code W}；
 * local yaml 可写 {@code BLACK} / {@code WHITE} 或短码，由 {@link #fromCode(String)} 解析。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
public enum IpListType {

    /** 黑名单。DB 短码 {@code B}。 */
    BLACK("B"),

    /** 白名单。DB 短码 {@code W}；命中后网关写入 {@code ingot.security.whitelisted}，跳过黑名单检查 / 挑战 / 限流。 */
    WHITE("W");

    private final String dbCode;

    IpListType(String dbCode) {
        this.dbCode = dbCode;
    }

    public String dbCode() {
        return dbCode;
    }

    /**
     * 解析名单类型，兼容 {@code B/W} 短码与 {@code BLACK/WHITE} 全名。
     */
    public static IpListType fromCode(String raw) {
        if (raw == null) {
            return BLACK;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (upper.isEmpty()) {
            return BLACK;
        }
        for (IpListType t : values()) {
            if (t.dbCode.equals(upper) || t.name().equals(upper)) {
                return t;
            }
        }
        return BLACK;
    }
}
