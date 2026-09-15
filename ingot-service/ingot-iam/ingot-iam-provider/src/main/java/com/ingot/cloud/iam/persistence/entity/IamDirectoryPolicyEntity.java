package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户通讯录策略引用，不复制默认策略条目。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_directory_policy", autoResultMap = true)
public class IamDirectoryPolicyEntity {
    /** 所属租户 ID。 */
    @TableId(value = "tenant_id", type = IdType.INPUT)
    private BigInteger tenantId;

    /** 默认通讯录策略版本 ID。 */
    @TableField("default_revision_id")
    private BigInteger defaultRevisionId;

    /** 固定为 DIRECTORY，用于引用完整性。 */
    @TableField("default_kind")
    private DefaultPolicyKind defaultKind;

    /** 默认可见范围；为空表示继承默认版本。 */
    @TableField("default_scope")
    private DirectoryDefaultScope defaultScope;

    /** 显式选择器 ID，仅 SELECTED 时存在。 */
    @TableField("default_selector_id")
    private BigInteger defaultSelectorId;

    /** 递增配置版本。 */
    @TableField("version")
    private BigInteger version;
}
