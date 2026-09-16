package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamAppAudienceEntity;
import com.ingot.cloud.iam.persistence.entity.IamAudienceDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamAudienceGroupEntity;
import com.ingot.cloud.iam.persistence.entity.IamAudienceMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantAppEntitlementEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAppAudienceMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAudienceDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAudienceGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAudienceMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.AudienceKind;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护租户应用开通与可用人群行，不把开通写成操作授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class EntitlementRepository {
    private final IamTenantAppEntitlementMapper entitlements;
    private final IamAppAudienceMapper audiences;
    private final IamAudienceMemberMapper audienceMembers;
    private final IamAudienceDepartmentMapper audienceDepartments;
    private final IamAudienceGroupMapper audienceGroups;
    private final IamTenantMapper tenants;
    private final IamTenantMemberMapper members;
    private final IamDepartmentMapper departments;
    private final IamTenantGroupMapper groups;

    /**
     * 判断未删除组织是否恰好存在一行。
     *
     * @param tenantId 组织 ID
     * @return 存在时为 true
     */
    public boolean existsActiveTenant(long tenantId) {
        return tenants.selectCount(Wrappers.<IamTenantEntity>lambdaQuery()
                .eq(IamTenantEntity::getId, id(tenantId))
                .isNull(IamTenantEntity::getDeletedAt)) == 1;
    }

    /**
     * 分页列出组织开通，按应用 ID 排序。
     *
     * @param tenantId 已校验组织 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 开通页
     */
    public Page<IamTenantAppEntitlementEntity> pageEntitlements(long tenantId, int page, int pageSize) {
        return entitlements.selectPage(new Page<>(page, pageSize), Wrappers.<IamTenantAppEntitlementEntity>lambdaQuery()
                .eq(IamTenantAppEntitlementEntity::getTenantId, id(tenantId))
                .select(IamTenantAppEntitlementEntity::getId, IamTenantAppEntitlementEntity::getApplicationId,
                        IamTenantAppEntitlementEntity::getEnabled, IamTenantAppEntitlementEntity::getSource,
                        IamTenantAppEntitlementEntity::getSourceId, IamTenantAppEntitlementEntity::getValidFrom,
                        IamTenantAppEntitlementEntity::getValidUntil, IamTenantAppEntitlementEntity::getVersion)
                .orderByAsc(IamTenantAppEntitlementEntity::getApplicationId));
    }

    /**
     * 计算开通集合指纹 {@code applicationId:version:enabled}，无开通时为 {@code 0}。
     *
     * @param tenantId 已校验组织 ID
     * @return 集合版本
     */
    public String collectionVersion(long tenantId) {
        List<IamTenantAppEntitlementEntity> rows = entitlements.selectList(
                Wrappers.<IamTenantAppEntitlementEntity>lambdaQuery()
                        .eq(IamTenantAppEntitlementEntity::getTenantId, id(tenantId))
                        .select(IamTenantAppEntitlementEntity::getApplicationId,
                                IamTenantAppEntitlementEntity::getVersion, IamTenantAppEntitlementEntity::getEnabled)
                        .orderByAsc(IamTenantAppEntitlementEntity::getApplicationId));
        if (rows.isEmpty()) {
            return "0";
        }
        return rows.stream()
                .map(row -> row.getApplicationId() + ":" + version(row.getVersion()) + ":"
                        + (Boolean.TRUE.equals(row.getEnabled()) ? "1" : "0"))
                .collect(Collectors.joining("|"));
    }

    /**
     * 判断组织是否在当前数据库时钟下对该应用保持有效开通。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 恰好一行有效开通时为 true
     */
    public boolean entitled(long tenantId, long applicationId) {
        return entitlements.countEntitled(id(tenantId), id(applicationId)) == 1;
    }

    /**
     * 删除组织全部人群选择、人群配置与开通，须按外键依赖顺序调用。
     *
     * @param tenantId 已校验组织 ID
     */
    public void deleteAllForTenant(long tenantId) {
        BigInteger tenant = id(tenantId);
        audienceMembers.delete(Wrappers.<IamAudienceMemberEntity>lambdaQuery()
                .eq(IamAudienceMemberEntity::getTenantId, tenant));
        audienceDepartments.delete(Wrappers.<IamAudienceDepartmentEntity>lambdaQuery()
                .eq(IamAudienceDepartmentEntity::getTenantId, tenant));
        audienceGroups.delete(Wrappers.<IamAudienceGroupEntity>lambdaQuery()
                .eq(IamAudienceGroupEntity::getTenantId, tenant));
        audiences.delete(Wrappers.<IamAppAudienceEntity>lambdaQuery()
                .eq(IamAppAudienceEntity::getTenantId, tenant));
        entitlements.delete(Wrappers.<IamTenantAppEntitlementEntity>lambdaQuery()
                .eq(IamTenantAppEntitlementEntity::getTenantId, tenant));
    }

    /**
     * 插入开通行，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertEntitlement(IamTenantAppEntitlementEntity entity) {
        entitlements.insert(entity);
    }

    /**
     * 插入人群配置，启用与版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertAudience(IamAppAudienceEntity entity) {
        audiences.insert(entity);
    }

    /**
     * 读取应用人群种类与版本；行数不是恰好一行时为空。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 人群行，不存在或重复时为空
     */
    public IamAppAudienceEntity findAudience(long tenantId, long applicationId) {
        List<IamAppAudienceEntity> rows = audiences.selectList(Wrappers.<IamAppAudienceEntity>lambdaQuery()
                .eq(IamAppAudienceEntity::getTenantId, id(tenantId))
                .eq(IamAppAudienceEntity::getApplicationId, id(applicationId))
                .select(IamAppAudienceEntity::getAudienceKind, IamAppAudienceEntity::getVersion));
        return rows.size() == 1 ? rows.getFirst() : null;
    }

    /**
     * 删除指定应用的成员、部门与组选择，保留人群主行。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     */
    public void deleteAudienceSelections(long tenantId, long applicationId) {
        BigInteger tenant = id(tenantId);
        BigInteger application = id(applicationId);
        audienceMembers.delete(Wrappers.<IamAudienceMemberEntity>lambdaQuery()
                .eq(IamAudienceMemberEntity::getTenantId, tenant)
                .eq(IamAudienceMemberEntity::getApplicationId, application));
        audienceDepartments.delete(Wrappers.<IamAudienceDepartmentEntity>lambdaQuery()
                .eq(IamAudienceDepartmentEntity::getTenantId, tenant)
                .eq(IamAudienceDepartmentEntity::getApplicationId, application));
        audienceGroups.delete(Wrappers.<IamAudienceGroupEntity>lambdaQuery()
                .eq(IamAudienceGroupEntity::getTenantId, tenant)
                .eq(IamAudienceGroupEntity::getApplicationId, application));
    }

    /**
     * 更新人群种类并启用，将已读取的版本加一。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @param kind 人群种类
     * @param currentVersion 当前版本
     */
    public void updateAudience(long tenantId, long applicationId, AudienceKind kind, BigInteger currentVersion) {
        audiences.update(Wrappers.<IamAppAudienceEntity>lambdaUpdate()
                .eq(IamAppAudienceEntity::getTenantId, id(tenantId))
                .eq(IamAppAudienceEntity::getApplicationId, id(applicationId))
                .set(IamAppAudienceEntity::getAudienceKind, kind)
                .set(IamAppAudienceEntity::getEnabled, true)
                .set(IamAppAudienceEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 按成员 ID 排序读取人群成员。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 成员 ID
     */
    public List<BigInteger> audienceMemberIds(long tenantId, long applicationId) {
        return audienceMembers.selectList(Wrappers.<IamAudienceMemberEntity>lambdaQuery()
                        .eq(IamAudienceMemberEntity::getTenantId, id(tenantId))
                        .eq(IamAudienceMemberEntity::getApplicationId, id(applicationId))
                        .select(IamAudienceMemberEntity::getMemberId)
                        .orderByAsc(IamAudienceMemberEntity::getMemberId))
                .stream().map(IamAudienceMemberEntity::getMemberId).toList();
    }

    /**
     * 按部门 ID 排序读取人群部门选择。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 部门选择行
     */
    public List<IamAudienceDepartmentEntity> audienceDepartments(long tenantId, long applicationId) {
        return audienceDepartments.selectList(Wrappers.<IamAudienceDepartmentEntity>lambdaQuery()
                .eq(IamAudienceDepartmentEntity::getTenantId, id(tenantId))
                .eq(IamAudienceDepartmentEntity::getApplicationId, id(applicationId))
                .select(IamAudienceDepartmentEntity::getDepartmentId,
                        IamAudienceDepartmentEntity::getIncludeDescendants)
                .orderByAsc(IamAudienceDepartmentEntity::getDepartmentId));
    }

    /**
     * 按组 ID 排序读取人群组。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 组 ID
     */
    public List<BigInteger> audienceGroupIds(long tenantId, long applicationId) {
        return audienceGroups.selectList(Wrappers.<IamAudienceGroupEntity>lambdaQuery()
                        .eq(IamAudienceGroupEntity::getTenantId, id(tenantId))
                        .eq(IamAudienceGroupEntity::getApplicationId, id(applicationId))
                        .select(IamAudienceGroupEntity::getGroupId)
                        .orderByAsc(IamAudienceGroupEntity::getGroupId))
                .stream().map(IamAudienceGroupEntity::getGroupId).toList();
    }

    /**
     * 判断未移出的租户成员是否恰好存在一行。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 存在时为 true
     */
    public boolean existsActiveMember(long tenantId, long memberId) {
        return members.selectCount(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, id(tenantId))
                .eq(IamTenantMemberEntity::getId, id(memberId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)) == 1;
    }

    /**
     * 判断当前租户部门是否恰好存在一行。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentId 部门 ID
     * @return 存在时为 true
     */
    public boolean existsDepartment(long tenantId, long departmentId) {
        return departments.selectCount(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getTenantId, id(tenantId))
                .eq(IamDepartmentEntity::getId, id(departmentId))) == 1;
    }

    /**
     * 判断当前租户组是否恰好存在一行。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 存在时为 true
     */
    public boolean existsGroup(long tenantId, long groupId) {
        return groups.selectCount(Wrappers.<IamTenantGroupEntity>lambdaQuery()
                .eq(IamTenantGroupEntity::getTenantId, id(tenantId))
                .eq(IamTenantGroupEntity::getId, id(groupId))) == 1;
    }

    /**
     * 插入人群成员引用。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @param memberId 租户成员 ID
     */
    public void insertAudienceMember(long tenantId, long applicationId, long memberId) {
        IamAudienceMemberEntity row = new IamAudienceMemberEntity();
        row.setTenantId(id(tenantId));
        row.setApplicationId(id(applicationId));
        row.setMemberId(id(memberId));
        audienceMembers.insert(row);
    }

    /**
     * 插入人群部门引用。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @param departmentId 部门 ID
     * @param includeDescendants 是否包含下级
     */
    public void insertAudienceDepartment(long tenantId, long applicationId, long departmentId,
                                         boolean includeDescendants) {
        IamAudienceDepartmentEntity row = new IamAudienceDepartmentEntity();
        row.setTenantId(id(tenantId));
        row.setApplicationId(id(applicationId));
        row.setDepartmentId(id(departmentId));
        row.setIncludeDescendants(includeDescendants);
        audienceDepartments.insert(row);
    }

    /**
     * 插入人群组引用。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @param groupId 租户组 ID
     */
    public void insertAudienceGroup(long tenantId, long applicationId, long groupId) {
        IamAudienceGroupEntity row = new IamAudienceGroupEntity();
        row.setTenantId(id(tenantId));
        row.setApplicationId(id(applicationId));
        row.setGroupId(id(groupId));
        audienceGroups.insert(row);
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }

    private static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }
}
