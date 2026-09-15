package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.EntitlementSource;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户应用开通，不隐含授予任何操作。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_tenant_app_entitlement", autoResultMap = true)
public class IamTenantAppEntitlementEntity {
    /** 开通记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 开通的应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 开通来源。 */
    @TableField("source")
    private EntitlementSource source;

    /** 来源套餐或迁移批次 ID，可空。 */
    @TableField("source_id")
    private BigInteger sourceId;

    /** 生效时间，UTC。 */
    @TableField("valid_from")
    private LocalDateTime validFrom;

    /** 失效时间，UTC；为空表示持续有效。 */
    @TableField("valid_until")
    private LocalDateTime validUntil;

    /** 递增配置版本。 */
    @TableField("version")
    private BigInteger version;
}
