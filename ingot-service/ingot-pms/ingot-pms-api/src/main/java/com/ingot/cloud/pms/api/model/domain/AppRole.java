package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Getter
@Setter
@TableName("app_role")
public class AppRole extends BaseModel<AppRole> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户
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
    private OrgTypeEnums type;

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
    private LocalDateTime deletedAt;
}
