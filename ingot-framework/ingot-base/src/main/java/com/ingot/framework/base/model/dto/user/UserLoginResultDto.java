package com.ingot.framework.base.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserLoginResultDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 上午10:46.</p>
 */
@Data
public class UserLoginResultDto implements Serializable {
    private String access_token;
    private int access_token_expire;
    private String refresh_token;
    private int refresh_token_expire;
    private String redirect_url;
}
