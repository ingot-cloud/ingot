package com.ingot.cloud.security.api.model.dto;

import com.ingot.cloud.security.api.model.enums.BlacklistEventAction;
import com.ingot.cloud.security.api.model.enums.BlacklistTriggerSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关自动封禁 / 解封 / 续期事件上报数据传输对象。
 *
 * <p>由网关在限流违规升级、手工解封等场景通过 Feign 异步上报至
 * {@code ingot-security}，持久化为 {@code gateway_blacklist_event} 审计记录，
 * 并可能同步写入 {@code gateway_ip_list} 黑名单。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistReportDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 封禁主体维度短码，与 {@code gateway_ip_list.key_type} 一致。
     * <p>常见取值：{@code IP}（客户端 IP）、{@code DV}（设备指纹）、{@code UI}（用户 ID）。</p>
     */
    private String keyType;

    /**
     * 封禁主体值，与 {@link #keyType} 对应的具体 IP、设备指纹或用户 ID。
     */
    private String keyValue;

    /**
     * 事件动作短码，对应 {@link BlacklistEventAction}：
     * <ul>
     *     <li>{@link BlacklistEventAction#BLOCK}（{@code B}）：封禁。</li>
     *     <li>{@link BlacklistEventAction#UNBLOCK}（{@code U}）：解封。</li>
     *     <li>{@link BlacklistEventAction#RENEW}（{@code R}）：续期（延长 TTL）。</li>
     * </ul>
     * 解析请使用 {@link BlacklistEventAction#fromCode(String)}。
     */
    private String action;

    /**
     * 触发来源短码，对应 {@link BlacklistTriggerSource}：
     * <ul>
     *     <li>{@link BlacklistTriggerSource#AUTO}（{@code A}）：自动触发（如限流违规升级）。</li>
     *     <li>{@link BlacklistTriggerSource#MANUAL}（{@code M}）：管理面手工操作。</li>
     * </ul>
     * 解析请使用 {@link BlacklistTriggerSource#fromCode(String)}。
     */
    private String triggerSource;

    /**
     * 触发本次事件的限流规则编码（{@code gateway_rate_limit_rule.code}），
     * 手工操作时可空。
     */
    private String ruleCode;

    /**
     * 触发窗口内的违规 / 命中次数，用于审计追溯自动封禁依据。
     */
    private Integer countInWindow;

    /**
     * 本次封禁或续期的 TTL（秒）；解封事件通常为空。
     */
    private Integer ttlSec;

    /**
     * 链路追踪 ID，关联分布式日志排查。
     */
    private String traceId;

    /**
     * 触发事件时的请求路径（含 query string 与否取决于网关实现）。
     */
    private String requestPath;

    /**
     * 触发事件时的 User-Agent 请求头。
     */
    private String userAgent;

    /**
     * 客户端真实 IP（{@code In-Inner-Client-Real-IP}），与 {@link #keyType} 为 IP 时互为佐证。
     */
    private String realIp;

    /**
     * 附加备注，供审计与管理面展示。
     */
    private String remark;
}
