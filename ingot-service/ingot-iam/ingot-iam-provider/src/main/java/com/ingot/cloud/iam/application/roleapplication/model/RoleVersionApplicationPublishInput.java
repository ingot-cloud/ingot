package com.ingot.cloud.iam.application.roleapplication.model;

import com.ingot.cloud.iam.application.role.model.RolePublishInput;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationSelection;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>原子发布新权限版本并准备应用，不直接更新分配。</p>
 *
 * @param publish 发布内容
 * @param selection 处理集合方式
 * @param sourceRevisionIds 来源版本集合
 * @param assignmentIds 分配集合
 * @param idempotencyKey 幂等键
 * @author jy
 * @since 1.0.0
 */
public record RoleVersionApplicationPublishInput(
        @NotNull @Valid RolePublishInput publish,
        @NotNull RoleVersionApplicationSelection selection,
        List<@NotBlank String> sourceRevisionIds,
        List<@NotBlank String> assignmentIds,
        @NotBlank @Size(max = 128) String idempotencyKey) {
}
