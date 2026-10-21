package com.ingot.framework.commons.model.download;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>下载任务文件的短时效预签名链接。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param url 预签名链接
 * @param filename 下载文件名
 * @param expiresAt 链接过期时间，UTC
 */
@Schema(description = "下载链接")
public record DownloadLink(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String url,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String filename,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt) {
}
