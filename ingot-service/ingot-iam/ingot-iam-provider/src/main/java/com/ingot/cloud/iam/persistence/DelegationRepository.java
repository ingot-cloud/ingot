package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamDelegationActionCeilingEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationActionCeilingMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.RoleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护不可拼接的委派额度及其子行，平台与租户边界由调用方传入的 domain 与 tenantId 显式限定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class DelegationRepository {
    private final IamDelegationGrantMapper grants;
    private final IamDelegationRoleRevisionMapper revisions;
    private final IamDelegationRecipientMemberMapper recipientMembers;
    private final IamDelegationRecipientDepartmentMapper recipientDepartments;
    private final IamDelegationActionCeilingMapper ceilings;
    private final IamRoleAssignmentMapper assignments;
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamRoleRevisionMapper roleRevisions;

    /**
     * 分页列出当前域委派。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 委派页
     */
    public Page<IamDelegationGrantEntity> page(AuthorizationDomain domain, Long tenantId, int page, int pageSize) {
        return grants.selectPage(new Page<>(page, pageSize), scoped(domain, tenantId)
                .orderByAsc(IamDelegationGrantEntity::getId));
    }

    /**
     * 读取当前域委派。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param id 委派 ID
     * @return 委派行，不存在时为空
     */
    public IamDelegationGrantEntity find(AuthorizationDomain domain, Long tenantId, long id) {
        return grants.selectOne(scoped(domain, tenantId)
                .eq(IamDelegationGrantEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 锁定当前域委派行，调用方须处于事务中。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param id 委派 ID
     * @return 命中的委派 ID，不存在时为空
     */
    public BigInteger lock(AuthorizationDomain domain, Long tenantId, long id) {
        BigInteger delegationId = BigInteger.valueOf(id);
        if (domain == AuthorizationDomain.PLATFORM) {
            return grants.lockPlatform(delegationId, domain);
        }
        return grants.lockTenant(delegationId, domain, BigInteger.valueOf(tenantId));
    }

    /**
     * 插入委派行，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insert(IamDelegationGrantEntity entity) {
        grants.insert(entity);
    }

    /**
     * 更新管理员、有效期与派生期限并将版本加一。
     *
     * @param id 委派 ID
     * @param platformAdmin 平台管理员，租户域为空
     * @param tenantAdmin 租户管理员，平台域为空
     * @param validFrom 生效时间，可空
     * @param validUntil 失效时间，可空
     * @param seconds 派生分配最长秒数
     * @param nanos 派生分配最长纳秒
     * @param currentVersion 锁定后的当前版本
     */
    public void update(long id, BigInteger platformAdmin, BigInteger tenantAdmin, LocalDateTime validFrom,
                       LocalDateTime validUntil, long seconds, int nanos, BigInteger currentVersion) {
        grants.update(Wrappers.<IamDelegationGrantEntity>lambdaUpdate()
                .eq(IamDelegationGrantEntity::getId, BigInteger.valueOf(id))
                .set(IamDelegationGrantEntity::getPlatformAdministratorId, platformAdmin)
                .set(IamDelegationGrantEntity::getTenantAdministratorId, tenantAdmin)
                .set(IamDelegationGrantEntity::getValidFrom, validFrom)
                .set(IamDelegationGrantEntity::getValidUntil, validUntil)
                .set(IamDelegationGrantEntity::getMaxAssignmentDurationSeconds, seconds)
                .set(IamDelegationGrantEntity::getMaxAssignmentDurationNanos, nanos)
                .set(IamDelegationGrantEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 撤销委派并将版本加一。
     *
     * @param id 委派 ID
     * @param currentVersion 锁定后的当前版本
     */
    public void revoke(long id, BigInteger currentVersion) {
        grants.update(Wrappers.<IamDelegationGrantEntity>lambdaUpdate()
                .eq(IamDelegationGrantEntity::getId, BigInteger.valueOf(id))
                .set(IamDelegationGrantEntity::getStatus, GrantStatus.REVOKED)
                .set(IamDelegationGrantEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 撤销来源该委派的有效派生授权，并以 SQL 递增版本。
     *
     * @param delegationId 委派 ID
     */
    public void revokeDerivedAssignments(long delegationId) {
        assignments.update(Wrappers.<IamRoleAssignmentEntity>lambdaUpdate()
                .eq(IamRoleAssignmentEntity::getDelegationGrantId, BigInteger.valueOf(delegationId))
                .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE)
                .set(IamRoleAssignmentEntity::getStatus, GrantStatus.REVOKED)
                .setSql("version = version + 1"));
    }

    /**
     * 删除委派的角色、接收者与上限子行。
     *
     * @param delegationId 委派 ID
     */
    public void deleteChildren(long delegationId) {
        BigInteger id = BigInteger.valueOf(delegationId);
        revisions.delete(Wrappers.<IamDelegationRoleRevisionEntity>lambdaQuery()
                .eq(IamDelegationRoleRevisionEntity::getDelegationId, id));
        recipientMembers.delete(Wrappers.<IamDelegationRecipientMemberEntity>lambdaQuery()
                .eq(IamDelegationRecipientMemberEntity::getDelegationId, id));
        recipientDepartments.delete(Wrappers.<IamDelegationRecipientDepartmentEntity>lambdaQuery()
                .eq(IamDelegationRecipientDepartmentEntity::getDelegationId, id));
        ceilings.delete(Wrappers.<IamDelegationActionCeilingEntity>lambdaQuery()
                .eq(IamDelegationActionCeilingEntity::getDelegationId, id));
    }

    /**
     * 插入委派允许的角色版本。
     *
     * @param entity 待插入行
     */
    public void insertRoleRevision(IamDelegationRoleRevisionEntity entity) {
        revisions.insert(entity);
    }

    /**
     * 插入委派接收成员，不写入生成列。
     *
     * @param entity 待插入行
     */
    public void insertRecipientMember(IamDelegationRecipientMemberEntity entity) {
        recipientMembers.insert(entity);
    }

    /**
     * 插入委派接收部门。
     *
     * @param entity 待插入行
     */
    public void insertRecipientDepartment(IamDelegationRecipientDepartmentEntity entity) {
        recipientDepartments.insert(entity);
    }

    /**
     * 插入委派操作范围上限。
     *
     * @param entity 待插入行
     */
    public void insertCeiling(IamDelegationActionCeilingEntity entity) {
        ceilings.insert(entity);
    }

    /**
     * 按角色版本 ID 列出委派允许的角色。
     *
     * @param delegationId 委派 ID
     * @return 角色版本行
     */
    public List<IamDelegationRoleRevisionEntity> loadRoleRevisions(long delegationId) {
        return revisions.selectList(Wrappers.<IamDelegationRoleRevisionEntity>lambdaQuery()
                .eq(IamDelegationRoleRevisionEntity::getDelegationId, BigInteger.valueOf(delegationId))
                .orderByAsc(IamDelegationRoleRevisionEntity::getRevisionId));
    }

    /**
     * 列出委派接收成员。
     *
     * @param delegationId 委派 ID
     * @return 接收成员行
     */
    public List<IamDelegationRecipientMemberEntity> loadRecipientMembers(long delegationId) {
        return recipientMembers.selectList(Wrappers.<IamDelegationRecipientMemberEntity>lambdaQuery()
                .eq(IamDelegationRecipientMemberEntity::getDelegationId, BigInteger.valueOf(delegationId)));
    }

    /**
     * 按部门 ID 列出委派接收部门。
     *
     * @param delegationId 委派 ID
     * @return 接收部门行
     */
    public List<IamDelegationRecipientDepartmentEntity> loadRecipientDepartments(long delegationId) {
        return recipientDepartments.selectList(Wrappers.<IamDelegationRecipientDepartmentEntity>lambdaQuery()
                .eq(IamDelegationRecipientDepartmentEntity::getDelegationId, BigInteger.valueOf(delegationId))
                .orderByAsc(IamDelegationRecipientDepartmentEntity::getDepartmentId));
    }

    /**
     * 按操作 ID 列出委派范围上限。
     *
     * @param delegationId 委派 ID
     * @return 上限行
     */
    public List<IamDelegationActionCeilingEntity> loadCeilings(long delegationId) {
        return ceilings.selectList(Wrappers.<IamDelegationActionCeilingEntity>lambdaQuery()
                .eq(IamDelegationActionCeilingEntity::getDelegationId, BigInteger.valueOf(delegationId))
                .orderByAsc(IamDelegationActionCeilingEntity::getActionId));
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
     * 判断指定种类的角色版本是否存在。
     *
     * @param id 角色版本 ID
     * @param kind 角色种类
     * @return 存在时为 true
     */
    public boolean roleRevisionExists(long id, RoleKind kind) {
        return roleRevisions.selectCount(Wrappers.<IamRoleRevisionEntity>lambdaQuery()
                .eq(IamRoleRevisionEntity::getId, BigInteger.valueOf(id))
                .eq(IamRoleRevisionEntity::getKind, kind)) > 0;
    }

    /**
     * 列出仍有效的派生授权。
     *
     * @param delegationId 委派 ID
     * @return 派生分配行
     */
    public List<IamRoleAssignmentEntity> activeDerivedAssignments(long delegationId) {
        return assignments.selectList(Wrappers.<IamRoleAssignmentEntity>lambdaQuery()
                .eq(IamRoleAssignmentEntity::getDelegationGrantId, BigInteger.valueOf(delegationId))
                .eq(IamRoleAssignmentEntity::getStatus, GrantStatus.ACTIVE)
                .select(IamRoleAssignmentEntity::getId, IamRoleAssignmentEntity::getRevisionId,
                        IamRoleAssignmentEntity::getValidFrom, IamRoleAssignmentEntity::getValidUntil,
                        IamRoleAssignmentEntity::getVersion));
    }

    private static LambdaQueryWrapper<IamDelegationGrantEntity> scoped(AuthorizationDomain domain, Long tenantId) {
        LambdaQueryWrapper<IamDelegationGrantEntity> wrapper = Wrappers.<IamDelegationGrantEntity>lambdaQuery()
                .eq(IamDelegationGrantEntity::getDomain, domain);
        if (domain == AuthorizationDomain.PLATFORM) {
            wrapper.isNull(IamDelegationGrantEntity::getTenantId);
        } else {
            wrapper.eq(IamDelegationGrantEntity::getTenantId, BigInteger.valueOf(tenantId));
        }
        return wrapper;
    }
}
