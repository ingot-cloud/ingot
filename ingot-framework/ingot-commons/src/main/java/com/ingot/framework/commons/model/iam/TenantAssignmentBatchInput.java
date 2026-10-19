package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>原子提交同一完整委派来源的角色分配。</p>
 * @author jy
 * @since 1.0.0
 * @param items 完整结果或输入集合
 */
@Schema(description = "原子提交同一完整委派来源的角色分配", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record TenantAssignmentBatchInput(
        @NotEmpty List<@NotNull @Valid TenantAssignmentInput> items) {

    /** 拒绝退出版本字段和未知输入，避免旧契约被静默解释。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("当前授权契约不支持字段：" + field); }

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public TenantAssignmentBatchInput {
        if (items != null) items = List.copyOf(items);
    }
}
