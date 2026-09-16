package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回策略预览的可见成员样例与必要部门骨架。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 策略种类
 * @param members 经预览策略及操作者权限共同限制的成员样例
 * @param departments 可见部门和必要祖先骨架
 * @param restricted 是否存在不可向操作者披露的结果
 */
@Schema(description = "返回策略预览的可见成员样例与必要部门骨架")
public record PolicyPreviewResult(
        @NotNull @Valid @Schema(description = "策略种类", requiredMode = Schema.RequiredMode.REQUIRED)
        DefaultPolicyKind kind,
        @NotNull @Schema(description = "经预览策略及操作者权限共同限制的成员样例", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ResourceDetail<MemberRecord>> members,
        @NotNull @Schema(description = "可见部门和必要祖先骨架", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid DepartmentRecord> departments,
        @NotNull @Valid @Schema(description = "是否存在不可向操作者披露的结果", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean restricted) {

    /**
     * 固定输入集合快照；必填空引用由 Bean Validation 拒绝。
     */
    public PolicyPreviewResult {
        members = members == null ? null : Collections.unmodifiableList(new ArrayList<>(members));
        departments = departments == null ? null : Collections.unmodifiableList(new ArrayList<>(departments));
    }
}
