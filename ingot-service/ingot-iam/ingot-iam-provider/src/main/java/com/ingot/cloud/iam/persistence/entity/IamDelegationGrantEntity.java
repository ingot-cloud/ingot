package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存管理员委派额度，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_delegation_grant", autoResultMap = true)
public class IamDelegationGrantEntity {
    /** 委派 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 授权域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 所属租户 ID，平台域为空。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 平台管理员成员 ID。 */
    @TableField("platform_administrator_id")
    private BigInteger platformAdministratorId;

    /** 租户管理员成员 ID。 */
    @TableField("tenant_administrator_id")
    private BigInteger tenantAdministratorId;

    /** 生效时间，UTC。 */
    @TableField("valid_from")
    private LocalDateTime validFrom;

    /** 失效时间，UTC。 */
    @TableField("valid_until")
    private LocalDateTime validUntil;

    /** 派生分配最长秒数。 */
    @TableField("max_assignment_duration_seconds")
    private Long maxAssignmentDurationSeconds;

    /** 派生分配最长纳秒。 */
    @TableField("max_assignment_duration_nanos")
    private Integer maxAssignmentDurationNanos;

    /** 委派状态。 */
    @TableField("status")
    private GrantStatus status;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

}
