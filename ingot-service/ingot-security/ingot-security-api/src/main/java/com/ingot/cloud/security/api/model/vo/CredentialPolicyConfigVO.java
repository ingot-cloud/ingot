package com.ingot.cloud.security.api.model.vo;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

/**
 * CredentialPolicyConfigVO
 *
 * @author jy
 * @since 2026/1/30
 */
@Data
public class CredentialPolicyConfigVO implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 策略类型: STRENGTH(强度): 1, EXPIRATION(过期): 2, HISTORY(历史): 3
     */
    private String policyType;

    /**
     * 策略配置JSON
     */
    private Map<String, Object> policyConfig;

    /**
     * 优先级，数字越小优先级越高
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;

}
