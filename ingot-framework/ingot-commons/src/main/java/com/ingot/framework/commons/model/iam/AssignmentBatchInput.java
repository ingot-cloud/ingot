package com.ingot.framework.commons.model.iam;

import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>承载一次原子批量分配，任一条校验失败均不得保存。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 非空分配列表
 */
@Schema(description = "承载一次原子批量分配，任一条校验失败均不得保存")
public record AssignmentBatchInput(
        @NotEmpty @Schema(description = "非空分配列表", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid AssignmentInput> items) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public AssignmentBatchInput {
        if (items != null) {
            items = Collections.unmodifiableList(new ArrayList<>(items));
        }
    }
}
