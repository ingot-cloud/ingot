package com.ingot.cloud.pms.api.model.vo.user;

import com.ingot.cloud.pms.api.model.vo.client.OAuthClientSimpleVo;
import com.ingot.cloud.pms.api.model.vo.dept.DeptSimpleVo;
import com.ingot.cloud.pms.api.model.vo.role.RoleSimpleVo;
import com.ingot.cloud.pms.api.model.vo.tenant.TenantSimpleVo;
import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : UserNoPwdAllDetailVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/11.</p>
 * <p>Time         : 11:12 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserNoPwdAllDetailVo extends BaseVo {

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
     * 部门
     */
    private DeptSimpleVo dept;

    /**
     * 租户信息
     */
    private TenantSimpleVo tenant;

    /**
     * 角色信息
     */
    private List<RoleSimpleVo> role_list;

    /**
     * Client 信息
     */
    private List<OAuthClientSimpleVo> client_list;
}
