package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserAuthDetailParamsDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/11.</p>
 * <p>Time         : 4:16 PM.</p>
 */
@Data
public class UserAuthDetailParamsDto implements Serializable {
    private String clientId;
    private String username;
    private String tenantCode;
}
