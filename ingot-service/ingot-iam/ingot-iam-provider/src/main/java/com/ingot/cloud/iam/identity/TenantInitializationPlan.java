package com.ingot.cloud.iam.identity;

import java.time.Instant;
import java.util.List;

import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.EntitlementSource;

/**
 * <p>承载服务器生成的组织初始化标识及基础目录引用，不作为外部创建请求直接反序列化。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param tenantId 新组织 ID
 * @param name 组织名称
 * @param avatar 组织头像对象路径，可空
 * @param ownerAccountId 已存在的全局所有者账号 ID，不创建或改写凭证
 * @param ownerMemberId 新组织所有者成员 ID
 * @param ownerDisplayName 所有者在本组织的显示名
 * @param rootDepartmentId 新根部门 ID
 * @param rootDepartmentName 根部门名称
 * @param assignmentId 所有者治理授权 ID
 * @param governanceRevisionId 服务器选定的租户域系统治理版本
 * @param directoryRevisionId 固定通讯录默认版本
 * @param fieldRevisionId 固定字段默认版本
 * @param applications 服务器解析后的初始开通
 * @param planId 选用套餐，可空
 * @param auditId 本次事务审计 ID
 */
public record TenantInitializationPlan(long tenantId, String name, String avatar, long ownerAccountId,
                                       long ownerMemberId,
                                       String ownerDisplayName, long rootDepartmentId, String rootDepartmentName,
                                       long assignmentId, long governanceRevisionId, long directoryRevisionId,
                                       long fieldRevisionId, List<Application> applications, Long planId, long auditId) {
    /**
     * 校验服务器计划结构，避免空组织或重复基础开通；引用状态在事务中再次检查。
     * @throws IllegalArgumentException 标识非正数、名称无效或基础应用缺失/重复
     */
    public TenantInitializationPlan {
        if (tenantId <= 0 || ownerAccountId <= 0 || ownerMemberId <= 0 || rootDepartmentId <= 0
                || assignmentId <= 0 || governanceRevisionId <= 0 || directoryRevisionId <= 0
                || fieldRevisionId <= 0 || auditId <= 0 || invalidName(name) || invalidName(ownerDisplayName)
                || invalidName(rootDepartmentName) || applications == null || applications.isEmpty()) {
            throw new IllegalArgumentException("初始化计划必须包含有效身份、名称和基础目录引用");
        }
        applications = List.copyOf(applications);
        if (applications.stream().map(Application::applicationId).distinct().count() != applications.size()
                || applications.stream().map(Application::entitlementId).distinct().count() != applications.size()) {
            throw new IllegalArgumentException("初始化基础应用及开通 ID 不能重复");
        }
    }

    private static boolean invalidName(String name) {
        return name == null || name.isBlank() || name.length() > 128;
    }

    /**
     * <p>关联服务器确定的应用、来源、期限和新建开通标识。</p>
     *
     * @author jy
     * @since 1.0.0
     * @param applicationId 已启用的租户域应用 ID
     * @param entitlementId 本次新建的开通 ID
     * @param source 开通来源
     * @param sourceId 套餐 ID，仅 PLAN 时有值
     * @param status 开通状态
     * @param validFrom 生效时间，可空
     * @param validUntil 失效时间，可空
     */
    public record Application(long applicationId, long entitlementId, EntitlementSource source, Long sourceId,
                              ConfigurationStatus status, Instant validFrom, Instant validUntil) {
        /**
         * 验证应用及开通标识有效。
         * @throws IllegalArgumentException 标识非正数
         */
        public Application {
            if (applicationId <= 0 || entitlementId <= 0) {
                throw new IllegalArgumentException("应用和开通标识必须为正数");
            }
            if (source == null) {
                source = EntitlementSource.INITIALIZATION;
            }
            if (status == null) {
                status = ConfigurationStatus.ENABLED;
            }
        }

        /**
         * 兼容仅指定应用与开通标识的初始化行。
         *
         * @param applicationId 应用 ID
         * @param entitlementId 开通 ID
         */
        public Application(long applicationId, long entitlementId) {
            this(applicationId, entitlementId, EntitlementSource.INITIALIZATION, null, ConfigurationStatus.ENABLED,
                    null, null);
        }
    }
}
