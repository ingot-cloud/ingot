package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : HandleLogoutDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/27.</p>
 * <p>Time         : 下午3:39.</p>
 */
@Data
public class HandleLogoutDto implements Serializable {

    /**
     * 用户 token
     */
    private String accessToken;
}
