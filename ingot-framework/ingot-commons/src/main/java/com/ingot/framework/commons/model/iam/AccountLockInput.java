package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>手动锁定全局账号，期限与原因交给安全框架用例处理。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 读取获得的账号版本
 * @param reasonDetail 锁定原因说明
 * @param lockedUntil 到期时间；为空表示永久锁定
 */
@Schema(description = "手动锁定全局账号，期限与原因交给安全框架用例处理")
public record AccountLockInput(
        @NotBlank @Schema(description = "读取获得的账号版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "锁定原因说明")
        String reasonDetail,
        @Schema(description = "到期时间；为空表示永久锁定")
        Instant lockedUntil) {
}
