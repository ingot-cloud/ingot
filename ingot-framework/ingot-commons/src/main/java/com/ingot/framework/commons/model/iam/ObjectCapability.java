package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>表达当前对象上的操作能力，不能作为后续提交的授权凭证。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param allowed 当前计算是否允许
 * @param reasonCode 拒绝或受限原因，允许时可空
 * @param message 可展示中文说明，不泄露越界对象
 */
@Schema(description = "表达当前对象上的操作能力，不能作为后续提交的授权凭证")
public record ObjectCapability(
        @Schema(description = "当前计算是否允许")
        boolean allowed,
        @Schema(description = "拒绝或受限原因，允许时可空")
        IamReasonCode reasonCode,
        @Schema(description = "可展示中文说明，不泄露越界对象")
        String message) {
}
