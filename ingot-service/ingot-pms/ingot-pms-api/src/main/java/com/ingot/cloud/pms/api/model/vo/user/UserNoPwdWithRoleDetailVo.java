package com.ingot.cloud.pms.api.model.vo.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ingot.cloud.pms.api.model.vo.role.RoleSimpleVo;
import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : UserNoPwdWithRoleDetailVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 3:25 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserNoPwdWithRoleDetailVo extends BaseVo {

    /**
     * 所属租户
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenant_id;

    /**
     * 部门ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dept_id;

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

    /**
     * 状态
     */
    private String status;

    /**
     * 是否已删除
     */
    private Boolean is_deleted;

    /**
     * 角色列表
     */
    private List<RoleSimpleVo> role_list;
}
