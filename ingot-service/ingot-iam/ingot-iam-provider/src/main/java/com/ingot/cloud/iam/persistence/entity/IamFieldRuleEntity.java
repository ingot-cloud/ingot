package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户字段规则，匹配键为场景、字段、查看者与目标范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_field_rule", autoResultMap = true)
public class IamFieldRuleEntity {
    /** 规则 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 后台或通讯录场景。 */
    @TableField("scenario")
    private PolicyScenario scenario;

    /** 成员字段键。 */
    @TableField("field_key")
    private String fieldKey;

    /** 查看者选择器 ID。 */
    @TableField("viewer_selector_id")
    private BigInteger viewerSelectorId;

    /** 目标范围 JSON 数组。 */
    @TableField("target_scope")
    private String targetScope;

    /** 范围参数绑定 JSON 对象。 */
    @TableField("scope_bindings")
    private String scopeBindings;

    /** 字段可见程度。 */
    @TableField("visibility")
    private FieldVisibility visibility;

    /** 是否允许写入；仅 FULL 可为 true。 */
    @TableField("editable")
    private Boolean editable;
}
