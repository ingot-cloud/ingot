package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionSourceTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>平台权限实体，归属于应用，支持精确编码与通配编码（{@code :*} / {@code :**}）。</p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Getter
@Setter
@ToString
@TableName("platform_permission")
public class PlatformPermission extends BaseModel<PlatformPermission> implements PermissionType {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属应用
     */
    private Long appId;

    /**
     * 节点类型
     */
    private PermissionNodeTypeEnum nodeType;

    /**
     * 来源类型
     */
    private PermissionSourceTypeEnum sourceType;

    /**
     * 来源资源 ID
     */
    private Long sourceId;

    /**
     * 是否系统托管
     */
    private Boolean managed;

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
