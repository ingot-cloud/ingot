package com.ingot.cloud.iam.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>平台角色在指定权限与资源上的默认数据范围规则。</p>
 *
 * <p>CUSTOM 不得写入租户部门 ID；同角色、权限、资源、范围类型仅一行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@TableName(value = "platform_role_data_rule", autoResultMap = true)
public class PlatformRoleDataRule extends BaseModel<PlatformRoleDataRule> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 平台角色 ID
     */
    private Long roleId;

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
     * CUSTOM 时的部门 ID 列表；其它类型为空列表
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
