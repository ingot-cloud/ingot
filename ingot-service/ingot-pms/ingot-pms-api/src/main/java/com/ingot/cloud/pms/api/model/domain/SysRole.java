package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TenantTable
@TableName("sys_role")
public class SysRole extends BaseModel<SysRole> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 组ID
     */
    private Long groupId;

    /**
     * 角色名称
     */
    @NotBlank(message = "{SysRole.name}", groups = Group.Create.class)
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色类型
     */
    private OrgTypeEnum type;

    /**
     * 是否过滤部门
     */
    private Boolean filterDept;

    /**
     * 数据权限类型
     */
    private DataScopeTypeEnum scopeType;

    /**
     * 数据权限范围
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> scopes;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;


}
