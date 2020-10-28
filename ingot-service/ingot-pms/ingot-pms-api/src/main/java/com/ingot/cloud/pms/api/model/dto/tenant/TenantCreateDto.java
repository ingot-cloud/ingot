package com.ingot.cloud.pms.api.model.dto.tenant;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : TenantCreateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 4:27 PM.</p>
 */
@Data
public class TenantCreateDto implements Serializable {
    private String name;
    private String code;
}
