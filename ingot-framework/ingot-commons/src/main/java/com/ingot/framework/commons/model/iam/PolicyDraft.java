package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>按策略种类承载唯一的待预览配置。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 策略种类
 * @param directory 通讯录草稿，仅 DIRECTORY 携带
 * @param field 字段草稿，仅 FIELD 携带
 */
@Schema(description = "按策略种类承载唯一的待预览配置")
public record PolicyDraft(
        @NotNull @Valid @Schema(description = "策略种类", requiredMode = Schema.RequiredMode.REQUIRED)
        DefaultPolicyKind kind,
        @Valid @Schema(description = "通讯录草稿，仅 DIRECTORY 携带")
        DirectoryPolicyDraft directory,
        @Valid @Schema(description = "字段草稿，仅 FIELD 携带")
        FieldPolicyDraft field) {

    /**
     * 策略草稿种类必须与唯一配置一致。
     * @return 是否符合结构约束，实体归属仍须服务端校验
     */
    @AssertTrue(message = "策略草稿种类必须与唯一配置一致")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isKindCompatible() {
        return kind == null || (kind == DefaultPolicyKind.DIRECTORY
                ? directory != null && field == null : field != null && directory == null);
    }
}
