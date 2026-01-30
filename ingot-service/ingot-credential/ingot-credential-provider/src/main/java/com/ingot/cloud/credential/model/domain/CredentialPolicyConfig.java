package com.ingot.cloud.credential.model.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证策略配置
 *
 * @author jymot
 * @since 2026-01-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "credential_policy_config", autoResultMap = true)
public class CredentialPolicyConfig implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID，NULL表示全局默认策略
     */
    private Long tenantId;

    /**
     * 策略类型: STRENGTH(强度), EXPIRATION(过期), HISTORY(历史)
     */
    private CredentialPolicyType policyType;

    /**
     * 策略配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> policyConfig;

    /**
     * 优先级，数字越小优先级越高
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
