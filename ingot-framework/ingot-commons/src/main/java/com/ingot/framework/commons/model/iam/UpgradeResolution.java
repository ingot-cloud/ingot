package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>对升级预览中的稳定冲突键给出显式处置。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param key 与预览冲突相同的稳定键
 * @param choice 采用新基础、保留差异或替换范围
 * @param scopes 仅替换范围时提交；省略表示未提供
 */
@Schema(description = "对升级预览中的稳定冲突键给出显式处置")
public record UpgradeResolution(
        @NotBlank @Schema(description = "与预览冲突相同的稳定键", requiredMode = Schema.RequiredMode.REQUIRED)
        String key,
        @NotNull @Schema(description = "采用新基础、保留差异或替换范围", requiredMode = Schema.RequiredMode.REQUIRED)
        UpgradeResolutionChoice choice,
        @Schema(description = "仅替换范围时提交；省略表示未提供")
        List<@NotNull @Valid ScopeExpression> scopes) {

    /**
     * 复制范围集合；未提供的替换范围保持空引用供校验拒绝。
     */
    public UpgradeResolution {
        if (scopes != null) {
            scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        }
    }

    /**
     * 只有替换范围必须携带范围集合，其余处置不得夹带范围。
     *
     * @return 处置与范围字段是否匹配
     */
    @JsonIgnore
    @AssertTrue(message = "只有替换范围必须携带范围集合")
    @Schema(hidden = true)
    public boolean isChoiceCompatible() {
        if (choice == null) {
            return true;
        }
        return choice == UpgradeResolutionChoice.REPLACE_SCOPE ? scopes != null
                : scopes == null;
    }
}
