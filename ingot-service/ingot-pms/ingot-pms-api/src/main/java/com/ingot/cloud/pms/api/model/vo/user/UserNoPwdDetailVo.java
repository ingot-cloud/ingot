package com.ingot.cloud.pms.api.model.vo.user;

import com.ingot.cloud.pms.api.model.vo.dept.DeptSimpleVo;
import com.ingot.cloud.pms.api.model.vo.tenant.TenantSimpleVo;
import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserNoPwdDetailVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/9.</p>
 * <p>Time         : 3:06 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserNoPwdDetailVo extends BaseVo {

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
}
