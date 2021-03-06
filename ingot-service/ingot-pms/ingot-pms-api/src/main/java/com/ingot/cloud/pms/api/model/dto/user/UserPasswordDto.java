package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserPasswordDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/23.</p>
 * <p>Time         : 9:59 下午.</p>
 */
@Data
public class UserPasswordDto implements Serializable {
    private String password;
    private String newPassword;
}
