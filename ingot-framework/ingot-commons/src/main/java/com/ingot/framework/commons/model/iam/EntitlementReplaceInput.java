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
 * <p>提交套餐与自选开通覆盖，由服务器计算并集后整表替换。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 开通配置读取版本
 * @param planId 可选套餐；与自选开通取并集
 * @param entitlements 自选应用及期限覆盖，应用不得重复
 */
@Schema(description = "提交套餐与自选开通覆盖，由服务器计算并集后整表替换")
public record EntitlementReplaceInput(
        @NotBlank @Schema(description = "开通配置读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "可选套餐；与自选开通取并集")
        String planId,
        @NotNull @Schema(description = "自选应用及期限覆盖，应用不得重复", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid EntitlementDraft> entitlements) {

    /**
     * 复制开通清单。
     */
    public EntitlementReplaceInput {
        if (entitlements != null) {
            entitlements = Collections.unmodifiableList(new ArrayList<>(entitlements));
        }
    }

    /**
     * 同一应用不能出现多条开通。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "同一应用不能出现多条开通")
    @Schema(hidden = true)
    public boolean isApplicationUnique() {
        if (entitlements == null || entitlements.contains(null)) {
            return true;
        }
        return entitlements.stream().map(EntitlementDraft::applicationId).distinct().count() == entitlements.size();
    }
}
