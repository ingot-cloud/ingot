package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IP/设备/用户 黑白名单 VO。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class IpListItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 名单类型短码：{@code B}=黑名单 / {@code W}=白名单。
     * SDK 内由 {@code IpListType.fromCode} 解析。
     */
    private String listType;

    /**
     * 维度短码：{@code IP}/{@code DV}/{@code UI}/{@code CD}(CIDR)/{@code UA}/{@code RF}。
     * SDK 内由 {@code IpKeyType.fromCode} 解析为枚举全名。
     */
    private String keyType;

    private String keyValue;

    private String reason;

    /**
     * M=手工 / A=自动。
     */
    private String source;

    private LocalDateTime effectiveAt;

    private LocalDateTime expiresAt;

    private boolean enabled;

    private Long operatorId;

    private String operatorName;
}
