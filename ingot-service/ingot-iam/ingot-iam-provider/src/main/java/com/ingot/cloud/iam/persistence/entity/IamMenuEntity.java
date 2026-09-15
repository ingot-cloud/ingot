package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射菜单目录的持久化字段。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_menu", autoResultMap = true)
public class IamMenuEntity {
    /** 菜单 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 父菜单 ID，根菜单为空。 */
    @TableField("parent_id")
    private BigInteger parentId;

    /** 菜单名称。 */
    @TableField("name")
    private String name;

    /** 路由路径。 */
    @TableField("path")
    private String path;

    /** 视图路径。 */
    @TableField("view_path")
    private String viewPath;

    /** 路由名称。 */
    @TableField("route_name")
    private String routeName;

    /** 菜单图标。 */
    @TableField("icon")
    private String icon;

    /** 菜单类型。 */
    @TableField("kind")
    private MenuKind kind;

    /** 操作匹配方式。 */
    @TableField("match_mode")
    private ActionMatchMode matchMode;

    /** 访问方式。 */
    @TableField("access_mode")
    private MenuAccessMode accessMode;

    /** 排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

}
