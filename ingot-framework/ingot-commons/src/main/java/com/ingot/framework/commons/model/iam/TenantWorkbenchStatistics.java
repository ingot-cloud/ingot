package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>返回当前租户身份可读取的工作台统计，无权限的指标省略，合法空范围返回零。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param tenantId 当前可信组织 ID
 * @param tenantName 当前组织名称
 * @param tenantAvatar 当前组织头像时效链接，可空
 * @param memberCount 当前成员读取范围内的在册人数，含暂停、不含移出；无读取权限时为空
 * @param departmentCount 当前部门读取范围内的普通部门数，不含组织根；无读取权限时为空
 * @param groupCount 当前组织用户组数；无组读取权限时为空
 * @param applicationCount 当前身份具有可见页面入口的可用应用数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "当前租户工作台统计，指标沿用各自资源的读取范围")
public record TenantWorkbenchStatistics(
        String tenantId,
        String tenantName,
        String tenantAvatar,
        Long memberCount,
        Long departmentCount,
        Long groupCount,
        long applicationCount) {
}
