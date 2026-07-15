package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IP / 设备 / 用户黑白名单视图对象。
 *
 * <p>对应 DB 表 {@code gateway_ip_list}，网关 SDK 编译为内存索引，
 * 在请求链路最前段执行黑白名单匹配。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class IpListItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 名单类型短码：{@code B}=黑名单 / {@code W}=白名单。
     * SDK 内由 {@code IpListType.fromCode} 解析。
     * <p>白名单命中后网关写入 {@code ingot.security.whitelisted}，跳过后续黑名单检查、挑战及限流。</p>
     */
    private String listType;

    /**
     * 匹配维度短码：{@code IP}/{@code DV}/{@code UI}/{@code CD}(CIDR)/{@code UA}/{@code RF}。
     * SDK 内由 {@code IpKeyType.fromCode} 解析为枚举全名。
     *
     * <ul>
     *     <li>{@code IP}：精确 IP（{@code In-Inner-Client-Real-IP}）。</li>
     *     <li>{@code DV}：设备指纹（{@code In-Ca-Sig}）。</li>
     *     <li>{@code UI}：用户 ID（{@code In-Inner-User-Id}）。</li>
     *     <li>{@code CD}：CIDR 网段，客户端 IP 落在段内即命中。</li>
     *     <li>{@code UA}：User-Agent 正则（Java {@code Pattern.find}）。</li>
     *     <li>{@code RF}：Referer 正则（Java {@code Pattern.find}）。</li>
     * </ul>
     */
    private String keyType;

    /**
     * 匹配值：IP 地址、CIDR、设备指纹、用户 ID 或正则表达式，取决于 {@link #keyType}。
     */
    private String keyValue;

    /**
     * 加入名单的原因说明，供审计与管理面展示。
     */
    private String reason;

    /**
     * 记录来源：{@code M}=管理面手工录入 / {@code A}=网关自动封禁写入。
     */
    private String source;

    /**
     * 生效时间；{@code null} 表示立即生效。
     */
    private LocalDateTime effectiveAt;

    /**
     * 失效时间；{@code null} 表示永久有效（直至手工禁用或解封）。
     */
    private LocalDateTime expiresAt;

    /**
     * 是否启用；{@code false} 时网关编译阶段忽略该条目。
     */
    private boolean enabled;

    /**
     * 操作人用户 ID（手工录入时填写）。
     */
    private Long operatorId;

    /**
     * 操作人显示名称（手工录入时填写）。
     */
    private String operatorName;
}
