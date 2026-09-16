package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformGroupEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformGroupMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationActionCeilingMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformGroupMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantGroupMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.AssignmentSource;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.SubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护原子角色分配及其委派校验所需的行，平台与租户边界由调用方传入的 domain 与 tenantId 显式限定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AssignmentRepository {
    private final IamRoleAssignmentMapper assignments;
    private final IamDelegationGrantMapper delegations;
    private final IamDelegationRoleRevisionMapper delegationRevisions;
    private final IamDelegationActionCeilingMapper ceilings;
    private final IamPlatformGroupMemberMapper platformGroupMembers;
    private final IamTenantGroupMemberMapper tenantGroupMembers;
    private final IamPlatformMemberMapper platformMembers;
    private final IamPlatformGroupMapper platformGroups;
    private final IamTenantMemberMapper tenantMembers;
    private final IamTenantGroupMapper tenantGroups;
    private final IamRoleRevisionMapper revisions;

    /**
     * 分页列出当前域分配。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 分配页
     */
    public Page<IamRoleAssignmentEntity> page(AuthorizationDomain domain, Long tenantId, int page, int pageSize) {
        return assignments.selectPage(new Page<>(page, pageSize), scoped(domain, tenantId)
                .orderByAsc(IamRoleAssignmentEntity::getId));
    }

    /**
     * 读取当前域分配。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param id 分配 ID
     * @return 分配行，不存在时为空
     */
    public IamRoleAssignmentEntity find(AuthorizationDomain domain, Long tenantId, long id) {
        return assignments.selectOne(scoped(domain, tenantId)
                .eq(IamRoleAssignmentEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 锁定当前域分配行，调用方须处于事务中。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param id 分配 ID
     * @return 锁定行，不存在时为空
     */
    public IamRoleAssignmentEntity lock(AuthorizationDomain domain, Long tenantId, long id) {
        BigInteger assignmentId = BigInteger.valueOf(id);
        if (domain == AuthorizationDomain.PLATFORM) {
            return assignments.lockPlatform(assignmentId, domain);
        }
        return assignments.lockTenant(assignmentId, domain, BigInteger.valueOf(tenantId));
    }

    /**
     * 插入分配行，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insert(IamRoleAssignmentEntity entity) {
        assignments.insert(entity);
    }

    /**
     * 更新角色版本、范围与有效期并将版本加一。
     *
     * @param id 分配 ID
     * @param revisionId 角色版本 ID
     * @param revisionKind 角色版本种类
     * @param bindings 范围绑定 JSON
     * @param validFrom 生效时间
     * @param validUntil 失效时间，可空
     * @param currentVersion 锁定后的当前版本
     */
    public void update(long id, long revisionId, RoleKind revisionKind, String bindings, LocalDateTime validFrom,
                       LocalDateTime validUntil, BigInteger currentVersion) {
        assignments.update(Wrappers.<IamRoleAssignmentEntity>lambdaUpdate()
                .eq(IamRoleAssignmentEntity::getId, BigInteger.valueOf(id))
                .set(IamRoleAssignmentEntity::getRevisionId, BigInteger.valueOf(revisionId))
                .set(IamRoleAssignmentEntity::getRevisionKind, revisionKind)
                .set(IamRoleAssignmentEntity::getScopeBindings, bindings)
                .set(IamRoleAssignmentEntity::getValidFrom, validFrom)
                .set(IamRoleAssignmentEntity::getValidUntil, validUntil)
                .set(IamRoleAssignmentEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 按锁定时读到的版本条件撤销分配并将版本加一。
     *
     * @param id 分配 ID
     * @param currentVersion 锁定后的当前版本
     * @return 受影响行数；为 0 表示版本已被并发改写
     */
    public int revoke(long id, BigInteger currentVersion) {
        return assignments.update(Wrappers.<IamRoleAssignmentEntity>lambdaUpdate()
                .eq(IamRoleAssignmentEntity::getId, BigInteger.valueOf(id))
                .eq(IamRoleAssignmentEntity::getVersion, currentVersion)
                .set(IamRoleAssignmentEntity::getStatus, GrantStatus.REVOKED)
                .set(IamRoleAssignmentEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 读取当前域委派额度。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param id 委派 ID
     * @return 委派行，不存在时为空
     */
    public IamDelegationGrantEntity findDelegation(AuthorizationDomain domain, Long tenantId, long id) {
        LambdaQueryWrapper<IamDelegationGrantEntity> wrapper = Wrappers.<IamDelegationGrantEntity>lambdaQuery()
                .eq(IamDelegationGrantEntity::getId, BigInteger.valueOf(id))
                .eq(IamDelegationGrantEntity::getDomain, domain);
        if (domain == AuthorizationDomain.PLATFORM) {
            wrapper.isNull(IamDelegationGrantEntity::getTenantId);
        } else {
            wrapper.eq(IamDelegationGrantEntity::getTenantId, BigInteger.valueOf(tenantId));
        }
        return delegations.selectOne(wrapper);
    }

    /**
     * 读取委派逐操作范围上限。
     *
     * @param delegationId 委派 ID
     * @return 操作 ID 到上限；未登记上限的操作不出现
     */
    public List<AuthorizationEvalRows.Ceiling> loadCeilings(long delegationId) {
        return ceilings.listByDelegation(BigInteger.valueOf(delegationId));
    }

    /**
     * 判断委派是否允许指定角色版本。
     *
     * @param delegationId 委派 ID
     * @param revisionId 角色版本 ID
     * @return 允许时为 true
     */
    public boolean delegationAllowsRevision(long delegationId, long revisionId) {
        return delegationRevisions.selectCount(Wrappers.<IamDelegationRoleRevisionEntity>lambdaQuery()
                .eq(IamDelegationRoleRevisionEntity::getDelegationId, BigInteger.valueOf(delegationId))
                .eq(IamDelegationRoleRevisionEntity::getRevisionId, BigInteger.valueOf(revisionId))) > 0;
    }

    /**
     * 列出平台组成员。
     *
     * @param groupId 平台组 ID
     * @return 成员 ID
     */
    public List<BigInteger> platformGroupMemberIds(long groupId) {
        return platformGroupMembers.selectList(Wrappers.<IamPlatformGroupMemberEntity>lambdaQuery()
                        .eq(IamPlatformGroupMemberEntity::getGroupId, BigInteger.valueOf(groupId))
                        .select(IamPlatformGroupMemberEntity::getMemberId))
                .stream().map(IamPlatformGroupMemberEntity::getMemberId).toList();
    }

    /**
     * 列出当前租户组的有效成员，含按部门授予展开后的成员。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 租户组 ID
     * @return 去重后的成员 ID
     */
    public List<BigInteger> tenantGroupMemberIds(long tenantId, long groupId) {
        return tenantGroupMembers.listMembership(BigInteger.valueOf(tenantId), BigInteger.valueOf(groupId));
    }

    /**
     * 判断未移出的平台成员是否存在。
     *
     * @param id 平台成员 ID
     * @return 存在时为 true
     */
    public boolean platformMemberExists(long id) {
        return platformMembers.selectCount(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(id))
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)) > 0;
    }

    /**
     * 判断平台组是否存在。
     *
     * @param id 平台组 ID
     * @return 存在时为 true
     */
    public boolean platformGroupExists(long id) {
        return platformGroups.selectCount(Wrappers.<IamPlatformGroupEntity>lambdaQuery()
                .eq(IamPlatformGroupEntity::getId, BigInteger.valueOf(id))) > 0;
    }

    /**
     * 判断当前租户未移出成员是否存在。
     *
     * @param tenantId 已授权租户 ID
     * @param id 租户成员 ID
     * @return 存在时为 true
     */
    public boolean tenantMemberExists(long tenantId, long id) {
        return tenantMembers.selectCount(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(id))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)) > 0;
    }

    /**
     * 判断当前租户组是否存在。
     *
     * @param tenantId 已授权租户 ID
     * @param id 租户组 ID
     * @return 存在时为 true
     */
    public boolean tenantGroupExists(long tenantId, long id) {
        return tenantGroups.selectCount(Wrappers.<IamTenantGroupEntity>lambdaQuery()
                .eq(IamTenantGroupEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantGroupEntity::getId, BigInteger.valueOf(id))) > 0;
    }

    /**
     * 读取角色版本及其定义归属。
     *
     * @param id 角色版本 ID
     * @return 联查投影，不存在时为空
     */
    public IamRoleRevisionJoin findRevision(long id) {
        return revisions.findWithDefinition(BigInteger.valueOf(id));
    }

    /**
     * 列出旧所有者名下仍有效的初始化系统治理授权，按 ID 升序以便后续按同一顺序加锁。
     *
     * @param tenantId 已授权租户 ID
     * @param ownerMemberId 当前所有者成员 ID
     * @return 待转交的分配行；可能为空
     */
    public List<IamRoleAssignmentEntity> listOwnerGovernance(long tenantId, long ownerMemberId) {
        return assignments.selectList(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .eq(IamRoleAssignmentEntity::getDomain, AuthorizationDomain.TENANT)
                .eq(IamRoleAssignmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.MEMBER)
                .eq(IamRoleAssignmentEntity::getTenantMemberId, BigInteger.valueOf(ownerMemberId))
                .eq(IamRoleAssignmentEntity::getSource, AssignmentSource.INITIALIZATION)
                .eq(IamRoleAssignmentEntity::getRevisionKind, RoleKind.SYSTEM)
                .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE)
                .orderByAsc(IamRoleAssignmentEntity::getId));
    }

    /**
     * 查找指定成员对某一角色版本的一条有效直接分配，不区分来源。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param revisionId 角色版本 ID
     * @return 命中的最小 ID 行；没有有效分配时为空
     */
    public IamRoleAssignmentEntity findActiveMemberRevision(long tenantId, long memberId, long revisionId) {
        return assignments.selectOne(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .eq(IamRoleAssignmentEntity::getDomain, AuthorizationDomain.TENANT)
                .eq(IamRoleAssignmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamRoleAssignmentEntity::getSubjectType, SubjectType.MEMBER)
                .eq(IamRoleAssignmentEntity::getTenantMemberId, BigInteger.valueOf(memberId))
                .eq(IamRoleAssignmentEntity::getRevisionId, BigInteger.valueOf(revisionId))
                .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE)
                .orderByAsc(IamRoleAssignmentEntity::getId)
                .last("LIMIT 1"));
    }

    private static LambdaQueryWrapper<IamRoleAssignmentEntity> scoped(AuthorizationDomain domain, Long tenantId) {
        LambdaQueryWrapper<IamRoleAssignmentEntity> wrapper = Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .eq(IamRoleAssignmentEntity::getDomain, domain);
        if (domain == AuthorizationDomain.PLATFORM) {
            wrapper.isNull(IamRoleAssignmentEntity::getTenantId);
        } else {
            wrapper.eq(IamRoleAssignmentEntity::getTenantId, BigInteger.valueOf(tenantId));
        }
        return wrapper;
    }
}
