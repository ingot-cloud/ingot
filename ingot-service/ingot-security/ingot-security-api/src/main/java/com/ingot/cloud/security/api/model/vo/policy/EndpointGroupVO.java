package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * API 路径分组视图对象。
 *
 * <p>对应 DB 表 {@code gateway_endpoint_group}，用于将一组 HTTP 路径模式
 * 抽象为可复用的分组，供限流规则、挑战策略等引用，避免重复维护路径列表。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class EndpointGroupVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 分组编码，全局唯一，被 {@code groupCode} 字段引用。
     */
    private String code;

    /**
     * 分组显示名称，供管理面展示。
     */
    private String name;

    /**
     * 本分组包含的 API 路径匹配列表，元素为 {@link EndpointPatternVO}。
     */
    private List<EndpointPatternVO> patternList;

    /**
     * 是否启用；{@code false} 时引用该分组的规则 / 策略不会匹配到本分组路径。
     */
    private boolean enabled;

    /**
     * 备注说明，仅供管理面展示。
     */
    private String remark;
}
