package com.ingot.cloud.pms.api.model.vo.role;

import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : RoleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/27.</p>
 * <p>Time         : 上午10:43.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleVo extends BaseVo {
    /**
     * 角色编码
     */
    private String role_code;

    /**
     * 角色名称
     */
    private String role_name;

    /**
     * 角色状态
     */
    private String status;

    /**
     * 角色备注
     */
    private String remark;

    /**
     * 角色类型
     */
    private String type;
}
