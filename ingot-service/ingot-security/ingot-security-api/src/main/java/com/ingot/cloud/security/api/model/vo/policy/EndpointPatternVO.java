package com.ingot.cloud.security.api.model.vo.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * API 路径匹配模式视图对象。
 *
 * <p>以 JSON 数组形式存储于 {@code gateway_endpoint_group.pattern_list}
 * 及 {@code gateway_rate_limit_rule.pattern_list} 等字段，
 * 网关 SDK 编译时转换为 Ant 风格或 Sentinel 路径谓词。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointPatternVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求路径模式，支持 Ant 通配（如 {@code /api/v1/**}）。
     */
    private String path;

    /**
     * HTTP 方法（如 {@code GET}、{@code POST}）；为空或 {@code *} 表示匹配任意方法。
     */
    private String method;
}
