package com.ingot.cloud.member.api.model.dto.user;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : MemberUserCreateByPhoneDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 17:23.</p>
 */
@Data
public class MemberUserCreateByPhoneDTO implements Serializable {
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
