package com.ingot.framework.gateway.rule.client.ratelimit.model;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * API 路径分组定义。
 *
 * <p>对应 {@code gateway_endpoint_group} 表。</p>
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

    private Long id;

    private String code;

    private String name;

    private List<EndpointPattern> patternList;

    private boolean enabled;

    private String remark;
}
