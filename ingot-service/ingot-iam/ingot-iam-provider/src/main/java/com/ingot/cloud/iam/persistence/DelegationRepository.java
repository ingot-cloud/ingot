package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.ingot.cloud.iam.support.IamFilters;
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
    private static final String PLATFORM_ADMINISTRATOR_NAME_FILTER = "EXISTS (SELECT 1 FROM iam_platform_member administrator"
            + " WHERE administrator.id = iam_delegation_grant.platform_administrator_id"
            + " AND administrator.display_name LIKE {0} ESCAPE '!')";
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
        return page(domain, tenantId, page, pageSize, null);
    }

    /**
     * 在管理域边界内按平台管理员显示名称筛选委派，再分页及统计。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param administratorName 平台管理员显示名称包含匹配，空白表示不限制
     * @return 委派页
     */
    public Page<IamDelegationGrantEntity> page(AuthorizationDomain domain, Long tenantId, int page, int pageSize,
                                                String administratorName) {
        String name = IamFilters.containsName(administratorName);
        String pattern = name == null ? null : "%" + name.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
        return grants.selectPage(new Page<>(page, pageSize), scoped(domain, tenantId)
                .apply(domain == AuthorizationDomain.PLATFORM && name != null,
                        PLATFORM_ADMINISTRATOR_NAME_FILTER, pattern)
                .orderByAsc(IamDelegationGrantEntity::getId));
    }

    /**
     * 批量读取已通过平台委派分页边界的管理员显示名称。
     *
     * @param administratorIds 已通过平台委派边界的成员 ID
     * @return 成员 ID 到显示名称的映射，成员缺失时无对应项
     */
    public Map<BigInteger, String> platformAdministratorNames(Collection<BigInteger> administratorIds) {
        if (administratorIds.isEmpty()) {
            return Map.of();
        }
        Map<BigInteger, String> result = new HashMap<>();
        platformMembers.selectList(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .in(IamPlatformMemberEntity::getId, administratorIds)
                .select(IamPlatformMemberEntity::getId, IamPlatformMemberEntity::getDisplayName))
                .forEach(member -> result.put(member.getId(), member.getDisplayName()));
        return result;
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
     * 批量加载已通过管理域筛选的委派子行，每类子行只查询一次。
     *
     * @param domain 可信管理域，平台不查询接收部门
     * @param ids 已通过 page 或 find 验证归属的委派 ID，允许为空
     * @return 按委派 ID 索引的子行；没有子行的委派返回空集合
     */
    public Map<BigInteger, Children> children(AuthorizationDomain domain, Collection<BigInteger> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var roleRows = revisions.selectList(Wrappers.<IamDelegationRoleRevisionEntity>lambdaQuery()
                .in(IamDelegationRoleRevisionEntity::getDelegationId, ids)
                .orderByAsc(IamDelegationRoleRevisionEntity::getRevisionId)).stream()
                .collect(Collectors.groupingBy(IamDelegationRoleRevisionEntity::getDelegationId));
        var memberRows = recipientMembers.selectList(Wrappers.<IamDelegationRecipientMemberEntity>lambdaQuery()
                .in(IamDelegationRecipientMemberEntity::getDelegationId, ids)).stream()
                .collect(Collectors.groupingBy(IamDelegationRecipientMemberEntity::getDelegationId));
        Map<BigInteger, List<IamDelegationRecipientDepartmentEntity>> departmentRows = domain == AuthorizationDomain.PLATFORM
                ? Map.of() : recipientDepartments.selectList(Wrappers.<IamDelegationRecipientDepartmentEntity>lambdaQuery()
                .in(IamDelegationRecipientDepartmentEntity::getDelegationId, ids)
                .orderByAsc(IamDelegationRecipientDepartmentEntity::getDepartmentId)).stream()
                .collect(Collectors.groupingBy(IamDelegationRecipientDepartmentEntity::getDelegationId));
        var ceilingRows = ceilings.selectList(Wrappers.<IamDelegationActionCeilingEntity>lambdaQuery()
                .in(IamDelegationActionCeilingEntity::getDelegationId, ids)
                .orderByAsc(IamDelegationActionCeilingEntity::getActionId)).stream()
                .collect(Collectors.groupingBy(IamDelegationActionCeilingEntity::getDelegationId));
        Map<BigInteger, Children> result = new LinkedHashMap<>();
        for (BigInteger id : ids) {
            result.put(id, new Children(roleRows.getOrDefault(id, List.of()), memberRows.getOrDefault(id, List.of()),
                    departmentRows.getOrDefault(id, List.of()), ceilingRows.getOrDefault(id, List.of())));
        }
        return result;
    }

    /**
     * <p>保存同一委派的批量加载子行，仅在本次响应组装内复用。</p>
     *
     * @param revisions 允许的固定角色版本
     * @param members 接收成员
     * @param departments 接收部门
     * @param ceilings 操作范围上限
     * @author jy
     * @since 1.0.0
     */
    public record Children(List<IamDelegationRoleRevisionEntity> revisions,
                           List<IamDelegationRecipientMemberEntity> members,
                           List<IamDelegationRecipientDepartmentEntity> departments,
                           List<IamDelegationActionCeilingEntity> ceilings) {
        /**
         * 固定子行集合，避免不同响应记录之间意外修改共享查询结果。
         */
        public Children {
            revisions = List.copyOf(revisions);
            members = List.copyOf(members);
            departments = List.copyOf(departments);
            ceilings = List.copyOf(ceilings);
        }
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
                .orderByAsc(IamRoleAssignmentEntity::getId));
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
