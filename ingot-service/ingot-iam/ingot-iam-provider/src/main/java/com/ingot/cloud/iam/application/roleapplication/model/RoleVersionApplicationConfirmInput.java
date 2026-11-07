package com.ingot.cloud.iam.application.roleapplication.model;

import jakarta.validation.constraints.*;

/**
 * <p>确认最新无冲突预览并异步执行。</p>
 *
 * @param expectedVersion 计划乐观锁
 * @param idempotencyKey 确认幂等键
 * @author jy
 * @since 1.0.0
 */
public record RoleVersionApplicationConfirmInput(
        @NotBlank String expectedVersion,
        @NotBlank @Size(max = 128) String idempotencyKey) {
}
