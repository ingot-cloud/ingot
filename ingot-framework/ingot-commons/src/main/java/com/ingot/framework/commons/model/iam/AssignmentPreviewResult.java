package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回原子分配批次的逐接收对象效果。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 顺序与请求一致；任一失败阻止整批提交
 */
@Schema(description = "返回原子分配批次的逐接收对象效果")
public record AssignmentPreviewResult(
        @NotNull @Schema(description = "顺序与请求一致；任一失败阻止整批提交", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid AssignmentPreviewItem> items) {

    /**
     * 固定输入集合快照；必填空引用由 Bean Validation 拒绝。
     */
    public AssignmentPreviewResult {
        items = items == null ? null : Collections.unmodifiableList(new ArrayList<>(items));
    }
}
