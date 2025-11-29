package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.cloud.pms.api.model.enums.PermissionTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
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
@TableName("meta_permission")
public class MetaPermission extends BaseModel<MetaPermission> implements PermissionType {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 权限名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 权限编码
     */
    @TableField("`code`")
    private String code;

    /**
     * 权限类型
     */
    @TableField("`type`")
    private PermissionTypeEnum type;

    /**
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 状态, 0:正常，9:禁用
     */
    @TableField("`status`")
    private CommonStatusEnum status;

    /**
     * 备注
     */
    private String remark;

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
