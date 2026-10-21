package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>返回表格任务产物的 Base64 内容，由调用方还原为文件下载。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param filename 下载文件名
 * @param contentType MIME
 * @param contentBase64 文件内容
 */
@Schema(description = "返回表格任务产物的 Base64 内容")
public record MemberWorkbookFile(
        @NotBlank @Schema(description = "下载文件名", requiredMode = Schema.RequiredMode.REQUIRED)
        String filename,
        @NotBlank @Schema(description = "MIME", requiredMode = Schema.RequiredMode.REQUIRED)
        String contentType,
        @NotBlank @Schema(description = "文件内容", requiredMode = Schema.RequiredMode.REQUIRED)
        String contentBase64) {
}
