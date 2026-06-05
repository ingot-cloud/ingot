package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * API 路径分组 VO。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class EndpointGroupVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private List<EndpointPatternVO> patternList;

    private boolean enabled;

    private String remark;
}
