package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回真实授权引擎的受限解释，不执行目标操作也不构成授权凭证。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param allowed 当前计算是否允许
 * @param reasonCode 拒绝原因，允许时可空
 * @param message 安全中文说明
 * @param sources 仅可披露来源，受限时可为空数组
 * @param scopeSummary 不包含越界对象明细的范围概括
 * @param fieldAccess 可披露字段限制，无字段时为空对象
 * @param version 计算使用的授权版本
 * @param expiresAt UTC 解释结果有效截止，提交仍重新鉴权
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回真实授权引擎的受限解释，不执行目标操作也不构成授权凭证")
public record Decision(
         @Schema(description = "当前计算是否允许", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean allowed,
         @Schema(description = "拒绝原因，允许时可空")
        IamReasonCode reasonCode,
        @NotBlank @Schema(description = "安全中文说明", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,
        @NotNull @Schema(description = "仅可披露来源，受限时可为空数组", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid DecisionSource> sources,
        @NotBlank @Schema(description = "不包含越界对象明细的范围概括", requiredMode = Schema.RequiredMode.REQUIRED)
        String scopeSummary,
        @NotNull @Schema(description = "可披露字段限制，无字段时为空对象", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid FieldAccess> fieldAccess,
        @NotBlank @Schema(description = "计算使用的授权版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 解释结果有效截止，提交仍重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public Decision {
        if (sources != null) {
            sources = Collections.unmodifiableList(new ArrayList<>(sources));
        }
        if (fieldAccess != null) {
            fieldAccess = Collections.unmodifiableMap(new LinkedHashMap<>(fieldAccess));
        }
    }
}
