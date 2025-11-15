package com.ingot.cloud.pms.api.model.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
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
@TableName(value = "tenant_role_private", autoResultMap = true)
public class TenantRolePrivate extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * PID
     */
    private Long pid;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 角色编码
     */
    @TableField("`code`")
    private String code;

    /**
     * 角色类型
     */
    @TableField("`type`")
    private RoleTypeEnum type;

    /**
     * 数据范围类型
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
    @TableField("`status`")
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
