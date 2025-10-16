package com.ingot.framework.commons.model.common;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * <p>Description  : AuthSuccessDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/27.</p>
 * <p>Time         : 10:38 PM.</p>
 */
@Data
public class AuthSuccessDTO implements Serializable {
    /**
     * 登录授权类型
     */
    private String grantType;
    /**
     * 登录账号
     */
    private String username;
    /**
     * 登录组织
     */
    private String org;
    /**
     * 登录IP
     */
    private String ip;
    /**
     * 登录时间
     */
    private LocalDateTime time;
}
