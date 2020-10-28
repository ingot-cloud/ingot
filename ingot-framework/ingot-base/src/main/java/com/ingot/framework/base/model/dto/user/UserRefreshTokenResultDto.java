package com.ingot.framework.base.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserRefreshTokenResultDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/6.</p>
 * <p>Time         : 10:35 AM.</p>
 */
@Data
public class UserRefreshTokenResultDto implements Serializable {
    private String access_token;
    private int access_token_expire;
}
