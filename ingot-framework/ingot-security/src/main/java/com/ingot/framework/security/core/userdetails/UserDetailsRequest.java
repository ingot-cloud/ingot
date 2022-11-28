package com.ingot.framework.security.core.userdetails;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : UserDetailsRequest.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:47 下午.</p>
 */
@Data
public class UserDetailsRequest implements Serializable {
    /**
     * 唯一编码，根据类型判断，可以是用户名或手机号或社交openId等
     */
    private String username;
}
