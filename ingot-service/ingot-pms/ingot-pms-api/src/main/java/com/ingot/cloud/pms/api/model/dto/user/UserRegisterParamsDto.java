package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : UserRegisterDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 11:38 AM.</p>
 */
@Data
public class UserRegisterParamsDto implements Serializable {
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 真实姓名
     */
    private String real_name;
    /**
     * 角色ID列表
     */
    private List<String> role_list;
    /**
     * 角色编码列表
     */
    private List<String> role_code_list;
    /**
     * 部门Id
     */
    private String dept_id;
    /**
     * 租户Id
     */
    private String tenant_id;
    /**
     * 租户编码
     */
    private String tenant_code;
}
