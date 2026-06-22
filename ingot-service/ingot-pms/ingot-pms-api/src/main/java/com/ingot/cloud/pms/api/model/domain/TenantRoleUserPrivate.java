package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>租户私有的角色-用户关联，按 {@code platformRole} 区分平台预设与租户自建角色。</p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Getter
@Setter
@ToString
@TenantTable
@TableName("tenant_role_user_private")
public class TenantRoleUserPrivate extends BaseModel<TenantRoleUserPrivate> {

    @Serial
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
     * 角色ID
     */
    private Long roleId;

    /**
     * 是否为平台角色
     */
    private Boolean platformRole;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 部门ID，可以为空，部门角色该字段不为空
     */
    private Long deptId;
}
