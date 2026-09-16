package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>声明诊断目标身份，服务端在当前域解析且限制可披露来源。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param memberId 当前域成员 ID，与 accountId 二选一
 * @param accountId 全局账号 ID，仅用于解析当前域成员，与 memberId 二选一
 * @param applicationId 应用 ID
 * @param actionId 精确操作 ID
 * @param targetId 可选目标对象 ID
 */
@Schema(description = "声明诊断目标身份，服务端在当前域解析且限制可披露来源")
public record DiagnoseInput(
        @Valid @Schema(description = "当前域成员 ID，与 accountId 二选一")
        String memberId,
        @Valid @Schema(description = "全局账号 ID，仅用于解析当前域成员，与 memberId 二选一")
        String accountId,
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotBlank @Schema(description = "精确操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionId,
        @Valid @Schema(description = "可选目标对象 ID")
        String targetId) {

    /**
     * 诊断必须且只能指定一个非空身份。
     * @return 是否符合结构约束，实体归属仍须服务端校验
     */
    @AssertTrue(message = "诊断必须且只能指定一个非空身份")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isIdentityExclusive() {
        return (memberId != null && !memberId.isBlank() && accountId == null)
                || (accountId != null && !accountId.isBlank() && memberId == null);
    }
}
