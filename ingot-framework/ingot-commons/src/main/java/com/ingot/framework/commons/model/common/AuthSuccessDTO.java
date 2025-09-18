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
    private String grantType;
    private String username;
    private String ip;
    private LocalDateTime time;
}
