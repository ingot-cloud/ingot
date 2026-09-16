package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回只读预览的校验、影响摘要及有效结果；版本不替代实际提交时的权限重验。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param <T> 预览结果类型
 * @param version 参与预览的版本字符串
 * @param valid 是否通过当前校验
 * @param errors 阻止提交的错误
 * @param warnings 不阻止提交的提示
 * @param impactSummary 仅包含调用者可见的影响统计
 * @param effectiveResult 可以生成时返回的有效结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Preview<T>(@NotBlank String version, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean valid,
        @NotNull List<@NotNull @Valid ValidationIssue> errors,
        @NotNull List<@NotNull @Valid ValidationIssue> warnings,
        @NotNull @Valid ImpactSummary impactSummary, @Valid T effectiveResult) {
    /**
     * 固定预览消息快照，不执行任何持久化操作。
     */
    public Preview {
        errors = errors == null ? null : Collections.unmodifiableList(new ArrayList<>(errors));
        warnings = warnings == null ? null : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /**
     * 校验有效标志与错误集合一致。
     * @return 集合存在且仅在没有阻断错误时有效
     */
    @AssertTrue(message = "预览有效标志必须与错误集合一致")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isConsistent() {
        return errors != null && valid == errors.isEmpty();
    }
}
