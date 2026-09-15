package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射应用目录的持久化字段。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_application", autoResultMap = true)
public class IamApplicationEntity {
    /** 应用 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 应用编码。 */
    @TableField("code")
    private String code;

    /** 所属授权域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 应用名称。 */
    @TableField("name")
    private String name;

    /** 应用说明。 */
    @TableField("description")
    private String description;

    /** 应用图标。 */
    @TableField("icon")
    private String icon;

    /** 排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否为租户基础应用。 */
    @TableField("baseline")
    private Boolean baseline;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

}
