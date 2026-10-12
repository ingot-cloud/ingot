package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>标识当前授权对应的固定发布版本，供详情展示和按版本进入编辑。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 固定版本 ID，不是角色 ID 或乐观锁
 * @param revision 发布序号，字符串传输
 * @param versionName 可读名称，未命名时可回退发布序号
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "当前授权对应的固定发布版本摘要，不含权限定义或历史差异")
public record RoleRevisionSummary(
        @NotBlank @Schema(description = "固定版本 ID", requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @NotBlank @Schema(description = "发布序号，字符串传输", requiredMode = Schema.RequiredMode.REQUIRED) String revision,
        @Schema(description = "可读版本名称") String versionName) {
}
