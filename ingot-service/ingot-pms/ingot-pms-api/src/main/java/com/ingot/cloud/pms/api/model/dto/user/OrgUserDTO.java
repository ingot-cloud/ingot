package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : 组织系统用户DTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/19.</p>
 * <p>Time         : 9:48 AM.</p>
 */
@Data
public class OrgUserDTO implements Serializable {
    /**
     * 部门ID
     */
    private List<Long> deptIds;

    /**
     * ID
     */
    private Long id;

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

    /**
     * 头像
     */
    private String avatar;

}
