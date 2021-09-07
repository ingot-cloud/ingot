package com.ingot.framework.security.core.userdetails;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserDetailsRequest.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:47 下午.</p>
 */
@Data
public class UserDetailsRequest implements Serializable {
    /**
     * 客户端ID
     */
    private String clientId;
    /**
     * 授权模式
     */
    private UserDetailsModeEnum mode;
    /**
     * 唯一编码，根据类型判断，可以是用户名或手机号或社交openId
     */
    private String uniqueCode;
}
