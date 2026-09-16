package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存应用内资源目录项，范围与字段能力以 JSON 存储。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_resource", autoResultMap = true)
public class IamResourceEntity {
    /** 资源 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 资源编码。 */
    @TableField("code")
    private String code;

    /** 资源名称。 */
    @TableField("name")
    private String name;

    /** 范围能力 JSON 数组。 */
    @TableField("scope_capabilities")
    private String scopeCapabilities;

    /** 字段能力 JSON 数组。 */
    @TableField("field_capabilities")
    private String fieldCapabilities;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

}
