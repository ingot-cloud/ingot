package com.ingot.framework.gateway.rule.client.ratelimit.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.*;

/**
 * 限流规则定义，对应表 {@code gateway_rate_limit_rule}。
 *
 * <p>{@link #groupCode} 与 {@link #patternList} 二选一：优先 {@code groupCode} 关联
 * {@link EndpointGroup}；为空则使用内联 {@code patternList}。</p>
 *
 * <p>yaml 示例见 {@link com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitRule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（remote 快照可能为空）。 */
    private Long id;

    /** 规则唯一编码，同时作为 Sentinel apiName / resource。 */
    private String code;

    /** 引用的 API 路径分组编码；与 {@link #patternList} 二选一。 */
    private String groupCode;

    /** 内联路径模式；{@link #groupCode} 非空时被忽略。 */
    private List<EndpointPattern> patternList;

    /** 限流维度：IP / DEVICE / USER，决定 Sentinel 参数流控的 Header 来源。 */
    private RateLimitDimension dimension;

    /** 平均 QPS（Sentinel {@code count}）。 */
    private int qps;

    /** 突发容量（Sentinel {@code burst}）。 */
    private int burst;

    /** 统计窗口秒数（Sentinel {@code intervalSec}，最小 1）。 */
    private int intervalSec;

    /**
     * 流控行为短码：{@link RateLimitControlBehavior#FAST_FAIL}（F）或
     * {@link RateLimitControlBehavior#QUEUE}（Q）。
     */
    private String controlBehavior;

    /** 为 false 时不编译进 Sentinel。 */
    private boolean enabled;

    /** 编译时排序权重，数值越小越先处理；不影响 Sentinel 运行期行为。 */
    private int priority;

    /** 管理面备注，执行面不读取。 */
    private String remark;
}
