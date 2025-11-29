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
 * <p>
 * 
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Getter
@Setter
@ToString
@TenantTable
@TableName("tenant_role_permission_private")
public class TenantRolePermissionPrivate extends BaseModel<TenantRolePermissionPrivate> {

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
     * 是否为元数据角色
     */
    private Boolean metaRole;

    /**
     * 权限ID
     */
    private Long permissionId;
}
