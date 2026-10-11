package com.ingot.framework.commons.model.iam;

import java.util.List;
import jakarta.validation.constraints.*;

/**
 * <p>准备固定版本的分配快照，标识全部由服务端验证。</p>
 *
 * @param targetRevisionId 目标已发布版本
 * @param selection 处理集合方式
 * @param sourceRevisionIds 指定来源版本集合
 * @param assignmentIds 指定分配集合
 * @param idempotencyKey 准备幂等键
 * @author jy
 * @since 1.0.0
 */
public record RoleApplicationPrepareInput(
        @NotBlank String targetRevisionId,
        @NotNull RoleApplicationSelection selection,
        List<@NotBlank String> sourceRevisionIds,
        List<@NotBlank String> assignmentIds,
        @NotBlank @Size(max = 128) String idempotencyKey) {
}
