package com.ingot.cloud.iam.infrastructure.persistence;

import java.util.stream.Collectors;
import java.util.Set;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentView;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>读取成员资格、联系方式和任职，租户过滤与对象范围先于分页。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {
    private static final int ID_BATCH = 200;
    private static final String DIRECT_DEPARTMENT_MEMBERSHIP = "SELECT 1 FROM iam_member_department md WHERE md.tenant_id = {0} AND md.member_id = iam_tenant_member.id AND md.department_id = {1}";
    private final IamAccountMapper accounts;
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamDepartmentMapper departments;
    private final IamMemberDepartmentMapper memberDepartments;


    /**
     * 分页列出未移出的平台成员。
     *
     * @param scope 已编译范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 成员页
     */
    public Page<IamPlatformMemberEntity> pagePlatform(ObjectScope scope, int page, int size) {
        return pagePlatform(scope, page, size, null, null, List.of());
    }

    /**
     * 分页列出平台成员。未指定资格时仍排除已移出；指定资格时按该值精确匹配。
     *
     * @param scope 已编译范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @param name 显示名包含匹配，空白表示不限制
     * @param status 成员资格，空表示不限制且排除已移出
     * @return 成员页
     */
    public Page<IamPlatformMemberEntity> pagePlatform(ObjectScope scope, int page, int size, String name,
                                                      MemberStatus status) {
        return pagePlatform(scope, page, size, name, status, List.of());
    }

    /**
     * 分页列出平台成员，可选按成员 ID 精确回显。
     *
     * @param scope 已编译范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @param name 显示名包含匹配，空白表示不限制
     * @param status 成员资格，空表示不限制且排除已移出
     * @param ids 成员 ID，空表示不按 ID 限制
     * @return 成员页
     */
    public Page<IamPlatformMemberEntity> pagePlatform(ObjectScope scope, int page, int size, String name,
                                                      MemberStatus status, Collection<Long> ids) {
        List<BigInteger> memberIds = ids == null || ids.isEmpty() ? List.of()
                : ids.stream().map(BigInteger::valueOf).toList();
        LambdaQueryWrapper<IamPlatformMemberEntity> wrapper = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .like(name != null && !name.isBlank(), IamPlatformMemberEntity::getDisplayName, name)
                .eq(status != null, IamPlatformMemberEntity::getStatus, status)
                .ne(status == null, IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)
                .in(!memberIds.isEmpty(), IamPlatformMemberEntity::getId, memberIds)
                .orderByAsc(IamPlatformMemberEntity::getId);
        ObjectScopeSql.restrictPlatformMembers(wrapper, scope);
        return platformMembers.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 分页列出指定平台用户组的直接成员，按组成员关系过滤。
     *
     * @param scope 已编译范围
     * @param groupId 平台组 ID
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @param name 显示名包含匹配，空白表示不限制
     * @return 成员页
     */
    public Page<IamPlatformMemberEntity> pagePlatformByGroup(ObjectScope scope, long groupId, int page, int size,
                                                             String name) {
        LambdaQueryWrapper<IamPlatformMemberEntity> wrapper = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .like(name != null && !name.isBlank(), IamPlatformMemberEntity::getDisplayName, name)
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)
                .apply("EXISTS (SELECT 1 FROM iam_platform_group_member gm WHERE gm.member_id = iam_platform_member.id"
                        + " AND gm.group_id = {0})", BigInteger.valueOf(groupId))
                .orderByAsc(IamPlatformMemberEntity::getId);
        ObjectScopeSql.restrictPlatformMembers(wrapper, scope);
        return platformMembers.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 为已通过对象范围校验的平台成员详情读取未删除账号的登录信息白名单。
     *
     * @param accountId 该成员关联的全局账号 ID
     * @return 仅包含账号 ID、登录名、登录手机号与最近登录时间；账号不存在或已删除时为空
     */
    public IamAccountEntity accountLoginInfo(BigInteger accountId) {
        return accounts.selectOne(Wrappers.<IamAccountEntity>lambdaQuery()
                .select(IamAccountEntity::getId, IamAccountEntity::getUsername, IamAccountEntity::getPhone,
                        IamAccountEntity::getLastLoginAt)
                .eq(IamAccountEntity::getId, accountId)
                .isNull(IamAccountEntity::getDeletedAt));
    }

    /**
     * 按账号 ID 读取未删除账号的联系资料。
     *
     * @param accountIds 全局账号 ID
     * @return 以账号 ID 索引的联系资料
     */
    public Map<BigInteger, IamAccountEntity> accountContacts(Collection<BigInteger> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Map.of();
        }
        List<IamAccountEntity> rows = accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                .select(IamAccountEntity::getId, IamAccountEntity::getUsername, IamAccountEntity::getPhone,
                        IamAccountEntity::getEmail)
                .in(IamAccountEntity::getId, accountIds)
                .isNull(IamAccountEntity::getDeletedAt));
        Map<BigInteger, IamAccountEntity> indexed = new LinkedHashMap<>();
        for (IamAccountEntity row : rows) {
            indexed.put(row.getId(), row);
        }
        return indexed;
    }

    /**
     * 分页列出当前租户未移出成员，筛选条件由调用方完成字段可见性校验。
     *
     * @param tenantId 已授权租户 ID
     * @param scope 已编译范围
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 成员页
     */
    public Page<IamTenantMemberEntity> pageTenant(long tenantId, ObjectScope scope, String phone, String email,
                                                  int page, int size) {
        return pageTenant(tenantId, scope, phone, email, null, null, page, size);
    }

    /**
     * 分页列出当前租户成员，可按直属部门与资格筛选；筛选条件由调用方完成字段可见性校验。
     *
     * @param tenantId 已授权租户 ID
     * @param scope 已编译范围
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param departmentId 直属任职部门，可空
     * @param status 成员资格，空表示排除已移出
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 成员页
     */
    public Page<IamTenantMemberEntity> pageTenant(long tenantId, ObjectScope scope, String phone, String email,
                                                  Long departmentId, MemberStatus status, int page, int size) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = tenantFilter(tenantId, scope, phone, email, departmentId,
                status)
                .orderByAsc(IamTenantMemberEntity::getId);
        return tenantMembers.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 分页列出范围内未移出的租户成员候选，按显示名包含匹配并可按 ID 回显。
     *
     * @param tenantId 已授权租户 ID
     * @param scope 已编译范围
     * @param name 显示名包含匹配，空白表示不限制
     * @param ids 成员 ID，空表示不按 ID 限制
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 成员页
     */
    public Page<IamTenantMemberEntity> pageTenantCandidates(long tenantId, ObjectScope scope, String name,
                                                            Collection<BigInteger> ids, int page, int size) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = tenantFilter(tenantId, scope, null, null, null, null)
                .like(name != null && !name.isBlank(), IamTenantMemberEntity::getDisplayName, name)
                .in(ids != null && !ids.isEmpty(), IamTenantMemberEntity::getId, ids)
                .orderByAsc(IamTenantMemberEntity::getId);
        return tenantMembers.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 按快照 ID 读取仍落入当前范围的未移出成员，条件全部下推到 SQL。
     *
     * @param tenantId 已授权租户 ID
     * @param scope 已编译范围
     * @param memberIds 快照成员 ID
     * @return 范围内成员，不保证输入顺序
     */
    public List<IamTenantMemberEntity> listTenantByIds(long tenantId, ObjectScope scope,
                                                       Collection<BigInteger> memberIds) {
        if (memberIds == null || memberIds.isEmpty() || scope.coversNone()) {
            return List.of();
        }
        List<IamTenantMemberEntity> rows = new ArrayList<>();
        List<BigInteger> batch = new ArrayList<>();
        for (BigInteger memberId : memberIds) {
            if (memberId == null) {
                continue;
            }
            batch.add(memberId);
            if (batch.size() == ID_BATCH) {
                rows.addAll(selectTenantBatch(tenantId, scope, batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            rows.addAll(selectTenantBatch(tenantId, scope, batch));
        }
        return rows;
    }

    private List<IamTenantMemberEntity> selectTenantBatch(long tenantId, ObjectScope scope,
                                                          Collection<BigInteger> memberIds) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)
                .in(IamTenantMemberEntity::getId, memberIds);
        ObjectScopeSql.restrictTenantMembers(wrapper, scope, BigInteger.valueOf(tenantId));
        return tenantMembers.selectList(wrapper);
    }

    /**
     * 按全局账号读取平台成员资格，包含暂停与已移出。
     *
     * @param accountId 全局账号 ID
     * @return 成员记录；该账号从未写入平台成员表时为空
     */
    public IamPlatformMemberEntity findPlatformByAccount(long accountId) {
        return platformMembers.selectOne(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .eq(IamPlatformMemberEntity::getAccountId, BigInteger.valueOf(accountId)));
    }

    /**
     * 按 ID 读取平台成员，包含已移出。
     *
     * @param memberId 平台成员 ID
     * @return 成员记录，不存在时为空
     */
    public IamPlatformMemberEntity findPlatform(long memberId) {
        return platformMembers.selectOne(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId)));
    }

    /**
     * 读取当前租户未移出成员。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 成员记录，不存在时为空
     */
    public IamTenantMemberEntity findTenant(long tenantId, long memberId) {
        return tenantMembers.selectOne(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED));
    }

    /**
     * 判断目标成员是否落入范围，用于详情可见性。
     *
     * @param domain 当前授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param memberId 目标成员
     * @param scope 已编译范围
     * @return 范围内且未移出时为 true
     */
    public boolean visible(AuthorizationDomain domain, Long tenantId, long memberId, ObjectScope scope) {
        if (scope.coversNone()) {
            return false;
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            LambdaQueryWrapper<IamPlatformMemberEntity> wrapper = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                    .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId))
                    .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED);
            ObjectScopeSql.restrictPlatformMembers(wrapper, scope);
            return platformMembers.selectCount(wrapper) > 0;
        }
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED);
        ObjectScopeSql.restrictTenantMembers(wrapper, scope, BigInteger.valueOf(tenantId));
        return tenantMembers.selectCount(wrapper) > 0;
    }

    /**
     * 按同一平台范围批量检查当前页未移出的成员，不替代关联列表或写入门禁。
     * @param ids 当前页成员 ID
     * @param scope 本操作的范围
     * @return 范围内成员 ID
     */
    public Set<BigInteger> visiblePlatformIds(Collection<BigInteger> ids, ObjectScope scope) {
        if (ids.isEmpty()) return Set.of();
        var query = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .select(IamPlatformMemberEntity::getId).in(IamPlatformMemberEntity::getId, ids)
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED);
        ObjectScopeSql.restrictPlatformMembers(query, scope);
        return platformMembers.selectList(query).stream().map(IamPlatformMemberEntity::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 按同一租户范围批量检查当前页未移出的成员，不替代写入门禁。
     *
     * @param tenantId 已授权租户 ID
     * @param ids 当前页成员 ID
     * @param scope 本操作的范围
     * @return 范围内且未移出的成员 ID
     */
    public Set<BigInteger> visibleTenantIds(long tenantId, Collection<BigInteger> ids, ObjectScope scope) {
        return visibleTenantIds(tenantId, ids, scope, false);
    }

    /** 已绑定资料回显可保留移出记录；仍按租户及完整成员SQL范围过滤。 */
    public Set<BigInteger> visibleTenantIds(long tenantId, Collection<BigInteger> ids, ObjectScope scope, boolean includeRemoved) {
        if (ids.isEmpty() || scope.coversNone()) {
            return Set.of();
        }
        var query = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .select(IamTenantMemberEntity::getId)
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .in(IamTenantMemberEntity::getId, ids);
        if (!includeRemoved) query.ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED);
        ObjectScopeSql.restrictTenantMembers(query, scope, BigInteger.valueOf(tenantId));
        return tenantMembers.selectList(query).stream().map(IamTenantMemberEntity::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 批量读取任职投影，避免列表路径对每个成员各查一次。
     *
     * @param tenantId 已授权租户 ID
     * @param memberIds 当前页成员
     * @return 成员 ID 到按主部门优先排序的任职
     */
    public Map<BigInteger, List<MemberDepartmentView>> departmentViews(long tenantId,
                                                                       Collection<BigInteger> memberIds) {
        Map<BigInteger, List<MemberDepartmentView>> result = new LinkedHashMap<>();
        if (memberIds == null || memberIds.isEmpty()) {
            return result;
        }
        BigInteger tenant = BigInteger.valueOf(tenantId);
        List<IamMemberDepartmentEntity> links = memberDepartments.selectList(
                Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .eq(IamMemberDepartmentEntity::getTenantId, tenant)
                        .in(IamMemberDepartmentEntity::getMemberId, memberIds));
        if (links.isEmpty()) {
            return result;
        }
        List<BigInteger> departmentIds = links.stream().map(IamMemberDepartmentEntity::getDepartmentId).distinct()
                .toList();
        Map<BigInteger, IamDepartmentEntity> names = new LinkedHashMap<>();
        for (IamDepartmentEntity department : departments.selectList(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .select(IamDepartmentEntity::getId, IamDepartmentEntity::getName)
                .eq(IamDepartmentEntity::getTenantId, tenant)
                .in(IamDepartmentEntity::getId, departmentIds))) {
            names.put(department.getId(), department);
        }
        links.sort(Comparator.comparing((IamMemberDepartmentEntity row) -> !Boolean.TRUE.equals(row.getIsPrimary()))
                .thenComparing(IamMemberDepartmentEntity::getDepartmentId));
        for (IamMemberDepartmentEntity link : links) {
            IamDepartmentEntity department = names.get(link.getDepartmentId());
            if (department == null) {
                continue;
            }
            result.computeIfAbsent(link.getMemberId(), key -> new ArrayList<>())
                    .add(new MemberDepartmentView(department.getId().toString(), department.getName(),
                            Boolean.TRUE.equals(link.getIsPrimary())));
        }
        return result;
    }

    /**
     * 检查启用且未删除的全局账号是否存在。
     *
     * @param accountId 全局账号 ID
     * @return 恰好一条时为 true
     */
    public boolean activeAccount(long accountId) {
        return accounts.selectCount(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getId, BigInteger.valueOf(accountId))
                .eq(IamAccountEntity::getEnabled, true)
                .isNull(IamAccountEntity::getDeletedAt)) == 1;
    }

    /**
     * 判断账号是否仍被未移出的平台或租户成员引用。
     *
     * @param accountId 全局账号 ID
     * @return 仍有成员资格时为 true
     */
    public boolean hasMembership(long accountId) {
        BigInteger id = BigInteger.valueOf(accountId);
        return platformMembers.selectCount(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .eq(IamPlatformMemberEntity::getAccountId, id)
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)) > 0
                || tenantMembers.selectCount(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getAccountId, id)
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)) > 0;
    }

    /**
     * 按成员列表相同的 SQL 范围统计在册人数，不读取成员资料或任职明细。
     *
     * @param tenantId 当前可信组织 ID
     * @param scope 成员读取范围
     * @return 含暂停、不含已移出的成员数
     */
    public long countTenant(long tenantId, ObjectScope scope) {
        return tenantMembers.selectCount(tenantFilter(tenantId, scope, null, null, null, null));
    }

    private LambdaQueryWrapper<IamTenantMemberEntity> tenantFilter(long tenantId, ObjectScope scope, String phone,
                                                                   String email, Long departmentId,
                                                                   MemberStatus status) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(status != null, IamTenantMemberEntity::getStatus, status)
                .ne(status == null, IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)
                .eq(phone != null && !phone.isBlank(), IamTenantMemberEntity::getPhone, phone)
                .eq(email != null && !email.isBlank(), IamTenantMemberEntity::getEmail, email);
        if (departmentId != null) {
            wrapper.exists(DIRECT_DEPARTMENT_MEMBERSHIP, BigInteger.valueOf(tenantId),
                    BigInteger.valueOf(departmentId));
        }
        ObjectScopeSql.restrictTenantMembers(wrapper, scope, BigInteger.valueOf(tenantId));
        return wrapper;
    }
}
