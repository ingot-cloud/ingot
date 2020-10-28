package com.ingot.cloud.pms.api.model.vo.authority;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ingot.framework.base.model.vo.TreeVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityWithRoleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/12.</p>
 * <p>Time         : 5:11 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthorityWithRoleVo extends TreeVo {

    /**
     * api路径
     */
    private String url;

    /**
     * 权限名称
     */
    private String authority_name;

    /**
     * 权限
     */
    private String authority_code;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 角色ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long role_id;

    /**
     * 角色编码
     */
    private String role_code;

    /**
     * 角色名称
     */
    private String role_name;

}
