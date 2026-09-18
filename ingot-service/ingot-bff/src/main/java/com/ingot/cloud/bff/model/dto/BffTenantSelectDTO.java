package com.ingot.cloud.bff.model.dto;

import lombok.Data;

/**
 * 选择租户：仅事务 ID 与组织 ID。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class BffTenantSelectDTO {
    private String transactionId;
    private String tenantId;
}
