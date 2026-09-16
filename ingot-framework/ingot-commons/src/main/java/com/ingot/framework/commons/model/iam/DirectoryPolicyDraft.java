package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述固定默认版本与本地通讯录规则，不产生有效授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param defaultRevisionId 固定的通讯录默认策略版本 ID
 * @param defaultOverride 可选本地默认范围；省略表示继承固定版本
 * @param rules 完整允许禁止规则；空数组清除本地规则
 */
@Schema(description = "描述固定默认版本与本地通讯录规则，不产生有效授权")
public record DirectoryPolicyDraft(
        @NotBlank @Schema(description = "固定的通讯录默认策略版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String defaultRevisionId,
        @Valid @Schema(description = "可选本地默认范围；省略表示继承固定版本")
        DirectoryDefault defaultOverride,
        @NotNull @Schema(description = "完整允许禁止规则；空数组清除本地规则", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid DirectoryRule> rules) {

    /**
     * 固定输入集合快照；必填空引用由 Bean Validation 拒绝。
     */
    public DirectoryPolicyDraft {
        rules = rules == null ? null : Collections.unmodifiableList(new ArrayList<>(rules));
    }
}
