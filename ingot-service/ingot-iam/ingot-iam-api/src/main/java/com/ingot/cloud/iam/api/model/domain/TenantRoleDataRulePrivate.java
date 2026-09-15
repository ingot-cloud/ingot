package com.ingot.cloud.iam.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>租户对角色追加的数据范围规则，不能删除或覆盖平台默认规则。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@TenantTable
@TableName(value = "tenant_role_data_rule_private", autoResultMap = true)
public class TenantRoleDataRulePrivate extends BaseModel<TenantRoleDataRulePrivate> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 角色 ID
     */
    private Long roleId;

    /**
     * 是否平台预设角色
     */
    private Boolean platformRole;

    /**
     * 授予的功能权限 ID
     */
    private Long permissionId;

    /**
     * 资源 ID
     */
    private Long resourceId;

    /**
     * 数据范围类型
     */
    private DataScopeTypeEnum scopeType;

    /**
     * CUSTOM 时的当前租户部门 ID 列表；其它类型为空列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> scopes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
