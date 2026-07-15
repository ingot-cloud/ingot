package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.security.api.model.enums.BlacklistEventAction;
import com.ingot.cloud.security.api.model.enums.BlacklistTriggerSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 封禁 / 解封 / 续期审计事件实体。
 *
 * <p>映射 DB 表 {@code gateway_blacklist_event}，记录网关自动封禁
 * 及管理面手工操作的全量审计轨迹，仅追加写入不可修改。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "gateway_blacklist_event")
public class GatewayBlacklistEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，雪花算法分配。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 封禁主体维度短码，与 {@code gateway_ip_list.key_type} 一致
     * （如 {@code IP}、{@code DV}、{@code UI}）。
     */
    private String keyType;

    /**
     * 封禁主体值，与 {@link #keyType} 对应的具体 IP、设备指纹或用户 ID。
     */
    private String keyValue;

    /**
     * 事件动作短码，对应 {@link BlacklistEventAction}：
     * {@link BlacklistEventAction#BLOCK}（{@code B}）封禁、
     * {@link BlacklistEventAction#UNBLOCK}（{@code U}）解封、
     * {@link BlacklistEventAction#RENEW}（{@code R}）续期。
     */
    private String action;

    /**
     * 触发来源短码，对应 {@link BlacklistTriggerSource}：
     * {@link BlacklistTriggerSource#AUTO}（{@code A}）自动 /
     * {@link BlacklistTriggerSource#MANUAL}（{@code M}）手工。
     */
    private String triggerSource;

    /**
     * 触发本次事件的限流规则编码（{@code gateway_rate_limit_rule.code}）。
     */
    private String ruleCode;

    /**
     * 触发窗口内的违规 / 命中次数。
     */
    private Integer countInWindow;

    /**
     * 本次封禁或续期的 TTL（秒）。
     */
    private Integer ttlSec;

    /**
     * 链路追踪 ID。
     */
    private String traceId;

    /**
     * 触发事件时的请求路径。
     */
    private String requestPath;

    /**
     * 触发事件时的 User-Agent 请求头。
     */
    private String userAgent;

    /**
     * 客户端真实 IP（{@code In-Inner-Client-Real-IP}）。
     */
    private String realIp;

    /**
     * 附加备注。
     */
    private String remark;

    /**
     * 事件创建时间（审计时间戳），插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
