package com.ingot.framework.commons.model.iam;

import java.util.Map;

/**
 * <p>角色版本应用计划及持久执行进度。</p>
 *
 * @param id 任务ID
 * @param targetRevisionNumber 目标系统版本号
 * @param targetVersionName 目标可读名称
 * @param roleId 角色ID
 * @param targetRevisionId 目标版本ID
 * @param status 计划状态
 * @param version 计划乐观锁
 * @param counts 各明细状态的数量
 * @param failureReason 暂停原因
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author jy
 * @since 1.0.0
 */
public record RoleApplicationTask(
        String id,
        String roleId,
        String targetRevisionId,
        RoleApplicationStatus status,
        String version,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(contentUsing = IamCountSerializer.class)
        Map<RoleApplicationItemStatus, Long> counts,
        String failureReason,
        java.time.Instant createdAt,
        java.time.Instant updatedAt, String targetRevisionNumber, String targetVersionName) {
}
