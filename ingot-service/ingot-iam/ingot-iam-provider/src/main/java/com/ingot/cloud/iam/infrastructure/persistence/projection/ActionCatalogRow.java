package com.ingot.cloud.iam.infrastructure.persistence.projection;

import java.math.BigInteger;

/**
 * <p>一次目录快照中的应用、可选资源与可选操作。</p>
 * @param applicationId 应用 ID
 * @param applicationCode 应用编码
 * @param applicationName 应用名称
 * @param resourceId 资源 ID，无资源时为空
 * @param resourceCode 资源编码
 * @param resourceName 资源名称
 * @param actionId 操作 ID，无操作时为空
 * @param actionCode 操作编码
 * @param actionName 操作名称
 * @param actionEnabled 操作状态
 * @author jy
 * @since 1.0.0
 */
public record ActionCatalogRow(BigInteger applicationId, String applicationCode, String applicationName,
                               BigInteger resourceId, String resourceCode, String resourceName,
                               BigInteger actionId, String actionCode, String actionName, Boolean actionEnabled) {
}
