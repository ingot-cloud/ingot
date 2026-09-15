package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.AudienceKind;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户应用可用人群配置，不单独构成授权。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_app_audience", autoResultMap = true)
public class IamAppAudienceEntity {
    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 所属应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 人群种类。 */
    @TableField("audience_kind")
    private AudienceKind audienceKind;

    /** 递增配置版本。 */
    @TableField("version")
    private BigInteger version;
}
