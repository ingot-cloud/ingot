package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : AppUserCreateDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 14:16.</p>
 */
@Data
public class AppUserCreateDTO implements Serializable {
    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;
}
