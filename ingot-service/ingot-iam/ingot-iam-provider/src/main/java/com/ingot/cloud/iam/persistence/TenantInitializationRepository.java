package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.identity.TenantInitializationPlan;
import com.ingot.cloud.iam.persistence.entity.IamAppAudienceEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDirectoryPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantAppEntitlementEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAppAudienceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDefaultPolicyRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDirectoryPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamOssPaths;
import com.ingot.framework.commons.model.iam.AssignmentSource;
import com.ingot.framework.commons.model.iam.AudienceKind;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.EntitlementSource;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.SubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>在同一事务中写入最小组织及基础引用，行锁与插入均使用类型化 Mapper。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class TenantInitializationRepository {
    private final IamAccountMapper accounts;
    private final IamRoleRevisionMapper revisions;
    private final IamApplicationMapper applications;
    private final IamDefaultPolicyRevisionMapper defaults;
    private final IamTenantMapper tenants;
    private final IamTenantMemberMapper members;
    private final IamDepartmentMapper departments;
    private final IamMemberDepartmentMapper memberDepartments;
    private final IamRoleAssignmentMapper assignments;
    private final IamTenantAppEntitlementMapper entitlements;
    private final IamAppAudienceMapper audiences;
    private final IamDirectoryPolicyMapper directoryPolicies;
    private final IamFieldPolicyMapper fieldPolicies;

    /**
     * 锁定启用且未删除的所有者账号。
     * @param accountId 全局账号 ID
     * @return 是否恰好命中一行
     */
    public boolean lockActiveAccount(long accountId) {
        return accounts.lockActive(BigInteger.valueOf(accountId)) != null;
    }

    /**
     * 锁定租户域启用的系统角色版本。
     * @param revisionId 角色版本 ID
     * @return 是否恰好命中一行
     */
    public boolean lockTenantSystemRevision(long revisionId) {
        return revisions.lockTenantSystem(BigInteger.valueOf(revisionId), RoleKind.SYSTEM,
                AuthorizationDomain.TENANT) != null;
    }

    /**
     * 锁定启用的租户域应用。
     * @param applicationId 应用 ID
     * @return 是否恰好命中一行
     */
    public boolean lockTenantApplication(long applicationId) {
        return applications.lockEnabled(BigInteger.valueOf(applicationId), AuthorizationDomain.TENANT) != null;
    }

    /**
     * 锁定指定类别的默认策略版本。
     * @param revisionId 策略版本 ID
     * @param kind 策略类别
     * @return 是否恰好命中一行
     */
    public boolean lockDefaultRevision(long revisionId, DefaultPolicyKind kind) {
        return defaults.lockKind(BigInteger.valueOf(revisionId), kind) != null;
    }

    /**
     * 按服务器计划写入最小组织、所有者、根部门、治理授权、开通与默认策略引用。
     * @param plan 已通过目录解析的初始化计划
     * @return 新组织初始版本
     */
    public String insertOrganization(TenantInitializationPlan plan) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        IamTenantEntity tenant = new IamTenantEntity();
        tenant.setId(id(plan.tenantId()));
        tenant.setName(plan.name());
        tenant.setAvatar(IamOssPaths.store(plan.avatar()));
        tenant.setPlanId(plan.planId() == null ? null : id(plan.planId()));
        tenants.insert(tenant);
        IamTenantMemberEntity owner = new IamTenantMemberEntity();
        owner.setId(id(plan.ownerMemberId()));
        owner.setTenantId(id(plan.tenantId()));
        owner.setAccountId(id(plan.ownerAccountId()));
        owner.setDisplayName(plan.ownerDisplayName());
        owner.setStatus(MemberStatus.ACTIVE);
        members.insert(owner);
        IamDepartmentEntity root = new IamDepartmentEntity();
        root.setId(id(plan.rootDepartmentId()));
        root.setTenantId(id(plan.tenantId()));
        root.setName(plan.rootDepartmentName());
        departments.insert(root);
        IamMemberDepartmentEntity relation = new IamMemberDepartmentEntity();
        relation.setTenantId(id(plan.tenantId()));
        relation.setMemberId(id(plan.ownerMemberId()));
        relation.setDepartmentId(id(plan.rootDepartmentId()));
        relation.setIsPrimary(true);
        memberDepartments.insert(relation);
        tenants.update(Wrappers.<IamTenantEntity>lambdaUpdate()
                .eq(IamTenantEntity::getId, id(plan.tenantId()))
                .set(IamTenantEntity::getOwnerMemberId, id(plan.ownerMemberId())));
        IamRoleAssignmentEntity assignment = new IamRoleAssignmentEntity();
        assignment.setId(id(plan.assignmentId()));
        assignment.setDomain(AuthorizationDomain.TENANT);
        assignment.setTenantId(id(plan.tenantId()));
        assignment.setSubjectType(SubjectType.MEMBER);
        assignment.setTenantMemberId(id(plan.ownerMemberId()));
        assignment.setRevisionId(id(plan.governanceRevisionId()));
        assignment.setRevisionKind(RoleKind.SYSTEM);
        assignment.setScopeBindings(IamJson.object(null));
        assignment.setValidFrom(now);
        assignment.setSource(AssignmentSource.INITIALIZATION);
        assignments.insert(assignment);
        for (TenantInitializationPlan.Application app : plan.applications()) {
            IamTenantAppEntitlementEntity entitlement = new IamTenantAppEntitlementEntity();
            entitlement.setId(id(app.entitlementId()));
            entitlement.setTenantId(id(plan.tenantId()));
            entitlement.setApplicationId(id(app.applicationId()));
            entitlement.setEnabled(app.status() != ConfigurationStatus.DISABLED);
            entitlement.setSource(app.source());
            entitlement.setSourceId(app.sourceId() == null ? null : id(app.sourceId()));
            entitlement.setValidFrom(app.validFrom() == null ? now : LocalDateTime.ofInstant(app.validFrom(), ZoneOffset.UTC));
            entitlement.setValidUntil(app.validUntil() == null ? null : LocalDateTime.ofInstant(app.validUntil(), ZoneOffset.UTC));
            entitlements.insert(entitlement);
            IamAppAudienceEntity audience = new IamAppAudienceEntity();
            audience.setTenantId(id(plan.tenantId()));
            audience.setApplicationId(id(app.applicationId()));
            audience.setAudienceKind(AudienceKind.ALL);
            audiences.insert(audience);
        }
        IamDirectoryPolicyEntity directory = new IamDirectoryPolicyEntity();
        directory.setTenantId(id(plan.tenantId()));
        directory.setDefaultRevisionId(id(plan.directoryRevisionId()));
        directoryPolicies.insert(directory);
        IamFieldPolicyEntity field = new IamFieldPolicyEntity();
        field.setTenantId(id(plan.tenantId()));
        field.setDefaultRevisionId(id(plan.fieldRevisionId()));
        fieldPolicies.insert(field);
        BigInteger version = tenants.currentVersion(id(plan.tenantId()));
        return version == null ? "0" : version.toString();
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
}
