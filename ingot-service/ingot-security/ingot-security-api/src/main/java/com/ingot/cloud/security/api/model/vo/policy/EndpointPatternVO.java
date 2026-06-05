package com.ingot.cloud.security.api.model.vo.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * API 路径模式 VO。
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

    private String path;

    private String method;
}
