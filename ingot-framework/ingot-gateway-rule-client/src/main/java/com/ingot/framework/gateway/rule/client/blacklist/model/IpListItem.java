package com.ingot.framework.gateway.rule.client.blacklist.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;

/**
 * 单条黑白名单条目。对应 {@code gateway_ip_list} 表。
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpListItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 名单类型。local yaml 写 {@code BLACK} / {@code WHITE}（或 {@code black}/{@code white}）；
     * remote 模式 DB 短码 {@code B}/{@code W} 由 {@link com.ingot.framework.gateway.rule.client.blacklist.model.IpListType#fromCode} 解析。
     */
    private IpListType listType;

    /**
     * 匹配维度。local yaml 写枚举全名（如 {@code DEVICE}）；DB 短码 IP/DV/UI/CD/UA/RF
     * 由 {@link com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType#fromCode} 解析。
     */
    private IpKeyType keyType;

    private String keyValue;

    private String reason;

    /**
     * 来源: M=手工 / A=自动。
     */
    private String source;

    private LocalDateTime effectiveAt;

    private LocalDateTime expiresAt;

    private boolean enabled;

    private Long operatorId;

    private String operatorName;
}
