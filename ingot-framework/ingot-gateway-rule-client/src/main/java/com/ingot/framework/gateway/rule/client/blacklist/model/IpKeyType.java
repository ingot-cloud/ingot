package com.ingot.framework.gateway.rule.client.blacklist.model;

import java.util.Locale;

/**
 * 黑白名单匹配维度。
 *
 * <p>DB 表 {@code gateway_ip_list.key_type} 为 {@code char(2)} 短码；
 * local yaml 可写枚举全名或短码，由 {@link #fromCode(String)} 统一解析。</p>
 *
 * <table>
 *   <tr><th>短码</th><th>枚举</th><th>匹配来源</th></tr>
 *   <tr><td>IP</td><td>{@link #IP}</td><td>{@code X-Client-Real-IP}</td></tr>
 *   <tr><td>DV</td><td>{@link #DEVICE}</td><td>{@code X-In-Ca-Sig}</td></tr>
 *   <tr><td>UI</td><td>{@link #USER}</td><td>{@code X-User-Id}</td></tr>
 *   <tr><td>CD</td><td>{@link #CIDR}</td><td>客户端 IP 是否落在 CIDR 段内</td></tr>
 *   <tr><td>UA</td><td>{@link #USER_AGENT}</td><td>User-Agent 正则（Java {@code Pattern.find}）</td></tr>
 *   <tr><td>RF</td><td>{@link #REFERER}</td><td>Referer 正则（Java {@code Pattern.find}）</td></tr>
 * </table>
 *
 * @author jy
 * @since 2026/5/26
 */
public enum IpKeyType {

    IP("IP"),
    DEVICE("DV"),
    USER("UI"),
    CIDR("CD"),
    USER_AGENT("UA"),
    REFERER("RF");

    private final String dbCode;

    IpKeyType(String dbCode) {
        this.dbCode = dbCode;
    }

    /** DB 字段 {@code char(2)} 短码。 */
    public String dbCode() {
        return dbCode;
    }

    /**
     * 解析维度字符串，同时兼容 DB 短码与枚举全名。
     * <p>大小写不敏感；为空或未知值时返回 {@link #IP}。</p>
     */
    public static IpKeyType fromCode(String raw) {
        if (raw == null) {
            return IP;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return IP;
        }
        String upper = s.toUpperCase(Locale.ROOT);
        for (IpKeyType t : values()) {
            if (t.dbCode.equals(upper) || t.name().equals(upper)) {
                return t;
            }
        }
        return IP;
    }
}
