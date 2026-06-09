package com.ingot.framework.gateway.rule.client.ratelimit.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.*;

/**
 * API 路径分组，供多条 {@link RateLimitRule} 或挑战策略复用同一组 path。
 *
 * <p>对应表 {@code gateway_endpoint_group}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointGroup implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（remote 快照可能为空）。 */
    private Long id;

    /** 分组唯一编码，被规则的 {@code groupCode} 引用。 */
    private String code;

    /** 分组展示名称，仅管理面使用。 */
    private String name;

    /** 本分组包含的网关路径模式列表（Ant 风格 path + method）。 */
    private List<EndpointPattern> patternList;

    /** 为 false 时编译阶段跳过该分组，其关联规则无有效 path。 */
    private boolean enabled;

    /** 管理面备注，执行面不读取。 */
    private String remark;
}
