package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述固定默认版本与完整字段限制规则。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param defaultRevisionId 固定的字段默认策略版本 ID
 * @param rules 完整字段规则；空数组恢复固定默认策略
 */
@Schema(description = "描述固定默认版本与完整字段限制规则")
public record FieldPolicyDraft(
        @NotBlank @Schema(description = "固定的字段默认策略版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String defaultRevisionId,
        @NotNull @Schema(description = "完整字段规则；空数组恢复固定默认策略", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldRule> rules) {

    /**
     * 固定输入集合快照；必填空引用由 Bean Validation 拒绝。
     */
    public FieldPolicyDraft {
        rules = rules == null ? null : Collections.unmodifiableList(new ArrayList<>(rules));
    }
}
