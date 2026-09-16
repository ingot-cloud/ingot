package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述独立于允许禁止规则的默认通讯录范围。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param scope 默认范围种类
 * @param selection SELECTED 时必填；其他范围不得携带
 */
@Schema(description = "描述独立于允许禁止规则的默认通讯录范围")
public record DirectoryDefault(
        @NotNull @Valid @Schema(description = "默认范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        DirectoryDefaultScope scope,
        @Valid @Schema(description = "SELECTED 时必填；其他范围不得携带")
        Selection selection) {

    /**
     * 仅 SELECTED 默认范围携带选择器。
     * @return 是否符合结构约束，实体归属仍须服务端校验
     */
    @AssertTrue(message = "仅 SELECTED 默认范围携带选择器")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSelectionCompatible() {
        return scope == null || (scope == DirectoryDefaultScope.SELECTED ? selection != null : selection == null);
    }
}
