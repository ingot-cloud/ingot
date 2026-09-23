package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformGroupEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformGroupMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformGroupMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.SubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护两域静态用户组及其成员、部门来源，租户操作始终绑定可信 tenantId。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class GroupRepository {
    private final IamPlatformGroupMapper platformGroups;
    private final IamTenantGroupMapper tenantGroups;
    private final IamPlatformGroupMemberMapper platformMembers;
    private final IamTenantGroupMemberMapper tenantMembers;
    private final IamTenantGroupDepartmentMapper departmentLinks;
    private final IamPlatformMemberMapper platformPeople;
    private final IamTenantMemberMapper tenantPeople;
    private final IamDepartmentMapper departments;
    private final IamMemberDepartmentMapper memberDepartments;
    private final IamRoleAssignmentMapper assignments;

    /**
     * 分页列出平台用户组。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 组页
     */
    public Page<IamPlatformGroupEntity> pagePlatform(int page, int pageSize) {
        return platformGroups.selectPage(new Page<>(page, pageSize), Wrappers.<IamPlatformGroupEntity>lambdaQuery()
                .orderByAsc(IamPlatformGroupEntity::getId));
    }

    /**
     * 按平台成员分页列出其所在用户组。
     *
     * @param memberId 平台成员 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 组页
     */
    public Page<IamPlatformGroupEntity> pagePlatformByMember(long memberId, int page, int pageSize) {
        Long total = platformMembers.selectCount(Wrappers.<IamPlatformGroupMemberEntity>lambdaQuery()
                .eq(IamPlatformGroupMemberEntity::getMemberId, BigInteger.valueOf(memberId)));
        Page<IamPlatformGroupEntity> result = new Page<>(page, pageSize, total == null ? 0 : total);
        if (total == null || total == 0) {
            return result;
        }
        int offset = Math.max(page - 1, 0) * pageSize;
        List<IamPlatformGroupMemberEntity> links = platformMembers.selectList(
                Wrappers.<IamPlatformGroupMemberEntity>lambdaQuery()
                        .eq(IamPlatformGroupMemberEntity::getMemberId, BigInteger.valueOf(memberId))
                        .orderByAsc(IamPlatformGroupMemberEntity::getGroupId)
                        .last("LIMIT " + pageSize + " OFFSET " + offset));
        if (links.isEmpty()) {
            return result;
        }
        List<BigInteger> ids = links.stream().map(IamPlatformGroupMemberEntity::getGroupId).toList();
        Map<BigInteger, IamPlatformGroupEntity> indexed = new LinkedHashMap<>();
        for (IamPlatformGroupEntity row : platformGroups.selectList(Wrappers.<IamPlatformGroupEntity>lambdaQuery()
                .in(IamPlatformGroupEntity::getId, ids))) {
            indexed.put(row.getId(), row);
        }
        result.setRecords(ids.stream().map(indexed::get).filter(Objects::nonNull).toList());
        return result;
    }

    /**
     * 分页列出当前租户用户组。
     *
     * @param tenantId 已授权租户 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 组页
     */
    public Page<IamTenantGroupEntity> pageTenant(long tenantId, int page, int pageSize) {
        return tenantGroups.selectPage(new Page<>(page, pageSize), Wrappers.<IamTenantGroupEntity>lambdaQuery()
                .eq(IamTenantGroupEntity::getTenantId, BigInteger.valueOf(tenantId))
                .orderByAsc(IamTenantGroupEntity::getId));
    }

    /**
     * 读取平台用户组。
     *
     * @param id 组 ID
     * @return 组行，不存在时为空
     */
    public IamPlatformGroupEntity findPlatform(long id) {
        return platformGroups.selectById(BigInteger.valueOf(id));
    }

    /**
     * 读取当前租户用户组。
     *
     * @param tenantId 已授权租户 ID
     * @param id 组 ID
     * @return 组行，不存在时为空
     */
    public IamTenantGroupEntity findTenant(long tenantId, long id) {
        return tenantGroups.selectOne(Wrappers.<IamTenantGroupEntity>lambdaQuery()
                .eq(IamTenantGroupEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantGroupEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 锁定平台用户组，调用方须处于事务中。
     *
     * @param id 组 ID
     * @return 锁定行，不存在时为空
     */
    public IamPlatformGroupEntity lockPlatform(long id) {
        return platformGroups.lockRow(BigInteger.valueOf(id));
    }

    /**
     * 锁定当前租户用户组，调用方须处于事务中。
     *
     * @param tenantId 已授权租户 ID
     * @param id 组 ID
     * @return 锁定行，不存在时为空
     */
    public IamTenantGroupEntity lockTenant(long tenantId, long id) {
        return tenantGroups.lockRow(BigInteger.valueOf(tenantId), BigInteger.valueOf(id));
    }

    /**
     * 插入平台用户组，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertPlatform(IamPlatformGroupEntity entity) {
        platformGroups.insert(entity);
    }

    /**
     * 插入租户用户组，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertTenant(IamTenantGroupEntity entity) {
        tenantGroups.insert(entity);
    }

    /**
     * 更新平台组展示信息并将版本加一。
     *
     * @param id 组 ID
     * @param name 名称
     * @param description 说明，可空
     * @param currentVersion 锁定后的当前版本
     */
    public void updatePlatform(long id, String name, String description, BigInteger currentVersion) {
        platformGroups.update(Wrappers.<IamPlatformGroupEntity>lambdaUpdate()
                .eq(IamPlatformGroupEntity::getId, BigInteger.valueOf(id))
                .set(IamPlatformGroupEntity::getName, name)
                .set(IamPlatformGroupEntity::getDescription, description)
                .set(IamPlatformGroupEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 更新当前租户组展示信息并将版本加一。
     *
     * @param tenantId 已授权租户 ID
     * @param id 组 ID
     * @param name 名称
     * @param description 说明，可空
     * @param currentVersion 锁定后的当前版本
     */
    public void updateTenant(long tenantId, long id, String name, String description, BigInteger currentVersion) {
        tenantGroups.update(Wrappers.<IamTenantGroupEntity>lambdaUpdate()
                .eq(IamTenantGroupEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantGroupEntity::getId, BigInteger.valueOf(id))
                .set(IamTenantGroupEntity::getName, name)
                .set(IamTenantGroupEntity::getDescription, description)
                .set(IamTenantGroupEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除平台用户组。
     *
     * @param id 组 ID
     */
    public void deletePlatform(long id) {
        platformGroups.deleteById(BigInteger.valueOf(id));
    }

    /**
     * 删除当前租户用户组。
     *
     * @param tenantId 已授权租户 ID
     * @param id 组 ID
     */
    public void deleteTenant(long tenantId, long id) {
        tenantGroups.delete(Wrappers.<IamTenantGroupEntity>lambdaQuery()
                .eq(IamTenantGroupEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantGroupEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 按成员 ID 升序读取平台组成员。
     *
     * @param groupId 组 ID
     * @return 成员 ID
     */
    public List<BigInteger> platformMemberIds(long groupId) {
        return platformMembers.selectList(Wrappers.<IamPlatformGroupMemberEntity>lambdaQuery()
                        .eq(IamPlatformGroupMemberEntity::getGroupId, BigInteger.valueOf(groupId))
                        .orderByAsc(IamPlatformGroupMemberEntity::getMemberId))
                .stream().map(IamPlatformGroupMemberEntity::getMemberId).toList();
    }

    /**
     * 按成员 ID 升序读取当前租户组成员。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 成员 ID
     */
    public List<BigInteger> tenantMemberIds(long tenantId, long groupId) {
        return tenantMembers.selectList(Wrappers.<IamTenantGroupMemberEntity>lambdaQuery()
                        .eq(IamTenantGroupMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                        .eq(IamTenantGroupMemberEntity::getGroupId, BigInteger.valueOf(groupId))
                        .orderByAsc(IamTenantGroupMemberEntity::getMemberId))
                .stream().map(IamTenantGroupMemberEntity::getMemberId).toList();
    }

    /**
     * 统计当前租户组的有效成员数，含部门来源及其下级，去重后计数。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 有效成员数
     */
    public long countTenantMembership(long tenantId, long groupId) {
        return tenantMembers.countMembership(BigInteger.valueOf(tenantId), BigInteger.valueOf(groupId));
    }

    /**
     * 按部门 ID 升序读取当前租户组的部门来源。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 部门来源行
     */
    public List<IamTenantGroupDepartmentEntity> tenantDepartments(long tenantId, long groupId) {
        return departmentLinks.selectList(Wrappers.<IamTenantGroupDepartmentEntity>lambdaQuery()
                .eq(IamTenantGroupDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantGroupDepartmentEntity::getGroupId, BigInteger.valueOf(groupId))
                .orderByAsc(IamTenantGroupDepartmentEntity::getDepartmentId));
    }

    /**
     * 清空平台组成员。
     *
     * @param groupId 组 ID
     */
    public void clearPlatformMembers(long groupId) {
        platformMembers.delete(Wrappers.<IamPlatformGroupMemberEntity>lambdaQuery()
                .eq(IamPlatformGroupMemberEntity::getGroupId, BigInteger.valueOf(groupId)));
    }

    /**
     * 清空当前租户组的成员与部门来源。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     */
    public void clearTenantSelection(long tenantId, long groupId) {
        BigInteger tenant = BigInteger.valueOf(tenantId);
        BigInteger group = BigInteger.valueOf(groupId);
        tenantMembers.delete(Wrappers.<IamTenantGroupMemberEntity>lambdaQuery()
                .eq(IamTenantGroupMemberEntity::getTenantId, tenant)
                .eq(IamTenantGroupMemberEntity::getGroupId, group));
        departmentLinks.delete(Wrappers.<IamTenantGroupDepartmentEntity>lambdaQuery()
                .eq(IamTenantGroupDepartmentEntity::getTenantId, tenant)
                .eq(IamTenantGroupDepartmentEntity::getGroupId, group));
    }

    /**
     * 插入平台组成员。
     *
     * @param groupId 组 ID
     * @param memberId 平台成员 ID
     */
    public void insertPlatformMember(long groupId, long memberId) {
        IamPlatformGroupMemberEntity row = new IamPlatformGroupMemberEntity();
        row.setGroupId(BigInteger.valueOf(groupId));
        row.setMemberId(BigInteger.valueOf(memberId));
        platformMembers.insert(row);
    }

    /**
     * 插入当前租户组成员。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @param memberId 租户成员 ID
     */
    public void insertTenantMember(long tenantId, long groupId, long memberId) {
        IamTenantGroupMemberEntity row = new IamTenantGroupMemberEntity();
        row.setTenantId(BigInteger.valueOf(tenantId));
        row.setGroupId(BigInteger.valueOf(groupId));
        row.setMemberId(BigInteger.valueOf(memberId));
        tenantMembers.insert(row);
    }

    /**
     * 插入当前租户组的部门来源。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @param departmentId 部门 ID
     * @param includeDescendants 是否包含下级
     */
    public void insertTenantDepartment(long tenantId, long groupId, long departmentId, boolean includeDescendants) {
        IamTenantGroupDepartmentEntity row = new IamTenantGroupDepartmentEntity();
        row.setTenantId(BigInteger.valueOf(tenantId));
        row.setGroupId(BigInteger.valueOf(groupId));
        row.setDepartmentId(BigInteger.valueOf(departmentId));
        row.setIncludeDescendants(includeDescendants);
        departmentLinks.insert(row);
    }

    /**
     * 判断平台成员是否存在且未移出。
     *
     * @param id 平台成员 ID
     * @return 存在未移出成员时为 true
     */
    public boolean existsActivePlatformMember(long id) {
        return platformPeople.selectCount(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(id))
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)) > 0;
    }

    /**
     * 判断当前租户成员是否存在且未移出。
     *
     * @param tenantId 已授权租户 ID
     * @param id 租户成员 ID
     * @return 存在未移出成员时为 true
     */
    public boolean existsActiveTenantMember(long tenantId, long id) {
        return tenantPeople.selectCount(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(id))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)) > 0;
    }

    /**
     * 判断当前租户部门是否存在。
     *
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @return 存在时为 true
     */
    public boolean existsDepartment(long tenantId, long id) {
        return departments.selectCount(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id))) > 0;
    }

    /**
     * 统计引用该平台组的授权条数。
     *
     * @param groupId 组 ID
     * @return 授权条数
     */
    public long countPlatformAssignments(long groupId) {
        return assignments.selectCount(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .eq(IamRoleAssignmentEntity::getPlatformGroupId, BigInteger.valueOf(groupId))
                .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.GROUP));
    }

    /**
     * 统计引用当前租户组的授权条数。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 授权条数
     */
    public long countTenantAssignments(long tenantId, long groupId) {
        return assignments.selectCount(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .eq(IamRoleAssignmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamRoleAssignmentEntity::getTenantGroupId, BigInteger.valueOf(groupId))
                .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.GROUP));
    }

    /**
     * 读取引用该平台组的有效授权 ID。
     *
     * @param groupId 组 ID
     * @return 授权 ID
     */
    public List<BigInteger> activePlatformAssignmentIds(long groupId) {
        return assignments.selectList(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                        .select(IamRoleAssignmentEntity::getId)
                        .eq(IamRoleAssignmentEntity::getPlatformGroupId, BigInteger.valueOf(groupId))
                        .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.GROUP)
                        .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE))
                .stream().map(IamRoleAssignmentEntity::getId).toList();
    }

    /**
     * 读取引用当前租户组的有效授权 ID。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 授权 ID
     */
    public List<BigInteger> activeTenantAssignmentIds(long tenantId, long groupId) {
        return assignments.selectList(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                        .select(IamRoleAssignmentEntity::getId)
                        .eq(IamRoleAssignmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                        .eq(IamRoleAssignmentEntity::getTenantGroupId, BigInteger.valueOf(groupId))
                        .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.GROUP)
                        .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE))
                .stream().map(IamRoleAssignmentEntity::getId).toList();
    }

    /**
     * 读取引用该平台组且来自委派的有效授权。
     *
     * @param groupId 组 ID
     * @return 授权 ID 与来源委派 ID
     */
    public List<IamRoleAssignmentEntity> delegatedPlatformAssignments(long groupId) {
        return assignments.selectList(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .select(IamRoleAssignmentEntity::getId, IamRoleAssignmentEntity::getDelegationGrantId)
                .eq(IamRoleAssignmentEntity::getPlatformGroupId, BigInteger.valueOf(groupId))
                .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.GROUP)
                .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE)
                .isNotNull(IamRoleAssignmentEntity::getDelegationGrantId));
    }

    /**
     * 读取引用当前租户组且来自委派的有效授权。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 授权 ID 与来源委派 ID
     */
    public List<IamRoleAssignmentEntity> delegatedTenantAssignments(long tenantId, long groupId) {
        return assignments.selectList(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .select(IamRoleAssignmentEntity::getId, IamRoleAssignmentEntity::getDelegationGrantId)
                .eq(IamRoleAssignmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamRoleAssignmentEntity::getTenantGroupId, BigInteger.valueOf(groupId))
                .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.GROUP)
                .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE)
                .isNotNull(IamRoleAssignmentEntity::getDelegationGrantId));
    }

    /**
     * 列出落在给定部门集合内的成员，用于在写入前展开待保存的组内容。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentIds 已展开的部门 ID；空集合返回空结果
     * @return 去重后的成员 ID
     */
    public List<BigInteger> membersInDepartments(long tenantId, Collection<BigInteger> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return List.of();
        }
        return memberDepartments.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .select(IamMemberDepartmentEntity::getMemberId)
                        .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                        .in(IamMemberDepartmentEntity::getDepartmentId, departmentIds))
                .stream().map(IamMemberDepartmentEntity::getMemberId).distinct().toList();
    }
}
