package com.ingot.cloud.iam.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.iam.api.model.enums.RoleTypeEnum;
import com.ingot.cloud.iam.api.model.types.RoleType;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
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
@TableName("tenant_role_private")
public class TenantRolePrivate extends BaseModel<TenantRolePrivate> implements RoleType {

    @Serial
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
     * 是否过滤部门
     */
    private Boolean filterDept;

    /**
     * 排序
     */
    private Integer sort;

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

    @Override
    public boolean getPlatformRole() {
        return false;
    }
}
