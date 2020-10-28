package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserUpdateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/28.</p>
 * <p>Time         : 10:51 AM.</p>
 */
@Data
public class UserUpdateDto implements Serializable {
    private String id;
    /**
     * 所属租户
     */
    private String tenant_id;

    /**
     * 部门ID
     */
    private String dept_id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名
     */
    private String real_name;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮件地址
     */
    private String email;
}
