package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2024-01-24
 */
@Getter
@Setter
@TableName("app_role_group")
public class AppRoleGroup extends Model<AppRoleGroup> {

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
     * 角色组名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 角色组类型
     */
    @TableField("`type`")
    private String type;
}
