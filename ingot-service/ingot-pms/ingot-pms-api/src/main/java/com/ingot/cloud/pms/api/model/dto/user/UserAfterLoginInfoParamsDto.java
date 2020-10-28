package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserAfterLoginInfoParamsDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 2:32 PM.</p>
 */
@Data
public class UserAfterLoginInfoParamsDto implements Serializable {
    private long tenantId;
    private String username;
    private String clientId;
}
