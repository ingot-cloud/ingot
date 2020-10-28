package com.ingot.cloud.pms.api.model.dto.tenant;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : TenantUpdateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 4:28 PM.</p>
 */
@Data
public class TenantUpdateDto implements Serializable {
    private String id;
    private String name;
    private String code;
    private String status;
}
