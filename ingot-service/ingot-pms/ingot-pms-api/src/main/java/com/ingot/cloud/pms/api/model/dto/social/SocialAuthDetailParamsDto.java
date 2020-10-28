package com.ingot.cloud.pms.api.model.dto.social;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : SocialAuthDetailParamsDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 09:37.</p>
 */
@Data
public class SocialAuthDetailParamsDto implements Serializable {
    private String type;
    private String code;
    private String clientId;
    private String tenantCode;
}
