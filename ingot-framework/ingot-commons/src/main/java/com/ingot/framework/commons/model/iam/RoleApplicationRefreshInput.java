package com.ingot.framework.commons.model.iam;

import jakarta.validation.constraints.*;

/**
 * <p>重新读取未完成项的最新事实并生成待确认预览。</p>
 *
 * @param expectedVersion 计划乐观锁
 * @author jy
 * @since 1.0.0
 */
public record RoleApplicationRefreshInput(
        @NotBlank String expectedVersion) {
}
