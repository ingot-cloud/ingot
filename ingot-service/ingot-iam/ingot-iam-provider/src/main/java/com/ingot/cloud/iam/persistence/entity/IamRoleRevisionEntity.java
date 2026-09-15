package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射已发布角色版本的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_revision", autoResultMap = true)
public class IamRoleRevisionEntity {
    /** 记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 角色定义 ID。 */
    @TableField("role_id")
    private BigInteger roleId;

    /** 角色种类。 */
    @TableField("kind")
    private RoleKind kind;

    /** 发布版本号。 */
    @TableField("revision")
    private BigInteger revision;

    /** 继承的基础角色版本 ID。 */
    @TableField("base_revision_id")
    private BigInteger baseRevisionId;

    /** 元数据覆盖 JSON 对象。 */
    @TableField("metadata_overrides")
    private String metadataOverrides;

    /** 发布时间，使用 UTC。 */
    @TableField("published_at")
    private LocalDateTime publishedAt;

}
