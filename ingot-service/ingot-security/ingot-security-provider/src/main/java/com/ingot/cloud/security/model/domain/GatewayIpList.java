package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IP / 设备 / 用户黑白名单实体。
 *
 * <p>映射 DB 表 {@code gateway_ip_list}，黑名单与白名单合表存储，
 * 通过 {@link #listType} 区分；网关 SDK 编译为内存索引后在请求链路最前段匹配。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "gateway_ip_list")
public class GatewayIpList implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，雪花算法分配。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 名单类型：{@code B}=黑名单 / {@code W}=白名单。
     * 解析请使用 SDK {@code IpListType.fromCode}。
     */
    private String listType;

    /**
     * 匹配维度短码：{@code IP} / {@code DV} / {@code UI} / {@code CD}(CIDR) / {@code UA} / {@code RF}。
     * 解析请使用 SDK {@code IpKeyType.fromCode}。
     */
    private String keyType;

    /**
     * 匹配值：IP 地址、CIDR 网段、设备指纹、用户 ID 或正则表达式。
     */
    private String keyValue;

    /**
     * 加入名单的原因说明。
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
     * 失效时间；{@code null} 表示永久有效。
     */
    private LocalDateTime expiresAt;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 操作人用户 ID（手工录入时填写）。
     */
    private Long operatorId;

    /**
     * 操作人显示名称（手工录入时填写）。
     */
    private String operatorName;

    /**
     * 记录创建时间，插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间，插入与更新时自动填充。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
