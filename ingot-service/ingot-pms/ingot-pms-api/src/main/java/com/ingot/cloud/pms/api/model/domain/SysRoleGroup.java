package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-09-20
 */
@Getter
@Setter
@TableName("sys_role_group")
public class SysRoleGroup extends BaseModel<SysRoleGroup> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色组名称
     */
    private String name;
}
