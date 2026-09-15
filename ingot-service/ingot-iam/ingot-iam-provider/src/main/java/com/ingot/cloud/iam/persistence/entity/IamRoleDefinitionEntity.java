package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射角色定义的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_definition", autoResultMap = true)
public class IamRoleDefinitionEntity {
    /** 记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 授权域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 角色种类。 */
    @TableField("kind")
    private RoleKind kind;

    /** 稳定编码。 */
    @TableField("code")
    private String code;

    /** 名称。 */
    @TableField("name")
    private String name;

    /** 说明。 */
    @TableField("description")
    private String description;

    /** 分组名称。 */
    @TableField("group_name")
    private String groupName;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

}
