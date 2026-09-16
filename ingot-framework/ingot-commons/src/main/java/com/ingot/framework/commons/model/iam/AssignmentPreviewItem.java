package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回一项分配的受限效果及不可提交原因。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param subject 待接收主体
 * @param allowed 当前预览是否可提交，提交时重验
 * @param errors 阻断原因
 * @param grants 可披露的生效操作与范围
 */
@Schema(description = "返回一项分配的受限效果及不可提交原因")
public record AssignmentPreviewItem(
        @NotNull @Valid @Schema(description = "待接收主体", requiredMode = Schema.RequiredMode.REQUIRED)
        SubjectRef subject,
        @NotNull @Valid @Schema(description = "当前预览是否可提交，提交时重验", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean allowed,
        @NotNull @Schema(description = "阻断原因", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ValidationIssue> errors,
        @NotNull @Schema(description = "可披露的生效操作与范围", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionGrant> grants) {

    /**
     * 固定输入集合快照；必填空引用由 Bean Validation 拒绝。
     */
    public AssignmentPreviewItem {
        errors = errors == null ? null : Collections.unmodifiableList(new ArrayList<>(errors));
        grants = grants == null ? null : Collections.unmodifiableList(new ArrayList<>(grants));
    }
}
