package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : AllOrgUserFilterDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/18.</p>
 * <p>Time         : 4:58 PM.</p>
 */
@Data
public class AllOrgUserFilterDTO implements Serializable {
    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮件地址
     */
    private String email;
}
