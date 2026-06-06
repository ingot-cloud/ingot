package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 网关限流规则视图对象。
 *
 * <p>对应 DB 表 {@code gateway_rate_limit_rule}，由 {@code InnerSecurityPolicyAPI}
 * 快照下发至网关 / 业务方，经 SDK 编译为 Sentinel 参数流控规则。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class RateLimitRuleVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 规则编码，全局唯一，供审计、封禁上报及运维检索引用。
     */
    private String code;

    /**
     * 关联的 API 路径分组编码（{@code gateway_endpoint_group.code}）。
     * <p>非空时优先使用分组内的 {@code patternList}；为空则使用本规则内联的 {@link #patternList}。</p>
     */
    private String groupCode;

    /**
     * 内联 API 路径匹配列表，元素为 {@link EndpointPatternVO}。
     * <p>当 {@link #groupCode} 为空时生效；与分组编码二选一或同时存在时以分组为准。</p>
     */
    private List<EndpointPatternVO> patternList;

    /**
     * 限流维度。同时兼容 DB 短码（IP/DV/UI）与枚举全名（IP/DEVICE/USER）。
     * SDK 内统一由 {@code RateLimitDimension.fromCode} 解析为枚举。
     *
     * <ul>
     *     <li>{@code IP}：按客户端真实 IP 限流（{@code X-Client-Real-IP}）。</li>
     *     <li>{@code DV}：按设备指纹限流（{@code X-In-Ca-Sig}）。</li>
     *     <li>{@code UI}：按用户 ID 限流（{@code X-User-Id}）；匿名请求退化为 API 整体限流。</li>
     * </ul>
     */
    private String dimension;

    /**
     * 平均请求速率上限（次/秒），对应 Sentinel QPS 阈值。
     */
    private int qps;

    /**
     * 突发容量（令牌桶大小），允许短时间内的流量峰值。
     */
    private int burst;

    /**
     * 统计窗口长度（秒），与 {@link #qps} 共同定义限流粒度。
     */
    private int intervalSec;

    /**
     * 流控行为：{@code F}=快速失败（直接拒绝）/ {@code Q}=排队等待。
     */
    private String controlBehavior;

    /**
     * 是否启用；{@code false} 时网关编译阶段跳过该规则。
     */
    private boolean enabled;

    /**
     * 匹配优先级，数值越小越优先；多条规则同时命中时按此字段排序。
     */
    private int priority;

    /**
     * 备注说明，仅供管理面展示，不参与网关执行逻辑。
     */
    private String remark;
}
