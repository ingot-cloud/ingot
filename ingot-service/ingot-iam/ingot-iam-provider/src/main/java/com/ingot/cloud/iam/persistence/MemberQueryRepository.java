package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>查询并维护当前域成员资格，租户操作始终绑定可信 tenantId 与类型化对象范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {
    private static final int ID_BATCH = 200;
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
        LambdaQueryWrapper<IamPlatformMemberEntity> wrapper = Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)
                .orderByAsc(IamPlatformMemberEntity::getId);
        ObjectScopeSql.restrictPlatformMembers(wrapper, scope);
        return platformMembers.selectPage(new Page<>(page, size), wrapper);
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
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = tenantFilter(tenantId, scope, phone, email)
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
     * 读取未移出的平台成员。
     *
     * @param memberId 平台成员 ID
     * @return 成员记录，不存在时为空
     */
    public IamPlatformMemberEntity findPlatform(long memberId) {
        return platformMembers.selectOne(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId))
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED));
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
     * 新增平台成员资格。
     *
     * @param id 新成员 ID
     * @param accountId 全局账号 ID
     * @param displayName 显示名称
     */
    public void insertPlatform(long id, long accountId, String displayName) {
        IamPlatformMemberEntity row = new IamPlatformMemberEntity();
        row.setId(BigInteger.valueOf(id));
        row.setAccountId(BigInteger.valueOf(accountId));
        row.setDisplayName(displayName);
        row.setStatus(MemberStatus.ACTIVE);
        platformMembers.insert(row);
    }

    /**
     * 新增当前租户成员资格。
     *
     * @param id 新成员 ID
     * @param tenantId 已授权租户 ID
     * @param accountId 全局账号 ID
     * @param displayName 显示名称
     */
    public void insertTenant(long id, long tenantId, long accountId, String displayName) {
        IamTenantMemberEntity row = new IamTenantMemberEntity();
        row.setId(BigInteger.valueOf(id));
        row.setTenantId(BigInteger.valueOf(tenantId));
        row.setAccountId(BigInteger.valueOf(accountId));
        row.setDisplayName(displayName);
        row.setStatus(MemberStatus.ACTIVE);
        tenantMembers.insert(row);
    }

    /**
     * 替换当前租户成员任职关系。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param departments 目标任职，键为部门 ID
     */
    public void replaceDepartments(long tenantId, long memberId, Map<String, Boolean> departments) {
        BigInteger tenant = BigInteger.valueOf(tenantId);
        BigInteger member = BigInteger.valueOf(memberId);
        memberDepartments.delete(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .eq(IamMemberDepartmentEntity::getTenantId, tenant)
                .eq(IamMemberDepartmentEntity::getMemberId, member));
        if (departments == null) {
            return;
        }
        departments.forEach((departmentId, primary) -> {
            IamMemberDepartmentEntity row = new IamMemberDepartmentEntity();
            row.setTenantId(tenant);
            row.setMemberId(member);
            row.setDepartmentId(new BigInteger(departmentId));
            row.setIsPrimary(Boolean.TRUE.equals(primary));
            memberDepartments.insert(row);
        });
    }

    /**
     * 在调用方事务中锁定未移出的平台成员。
     *
     * @param memberId 平台成员 ID
     * @return 锁定行，不存在或已移出时为空
     */
    public IamPlatformMemberEntity lockPlatform(long memberId) {
        IamPlatformMemberEntity row = platformMembers.lock(BigInteger.valueOf(memberId));
        return row == null || row.getStatus() == MemberStatus.REMOVED ? null : row;
    }

    /**
     * 在调用方事务中锁定未移出的租户成员。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 锁定行，不存在或已移出时为空
     */
    public IamTenantMemberEntity lockTenant(long tenantId, long memberId) {
        IamTenantMemberEntity row = tenantMembers.lock(BigInteger.valueOf(tenantId), BigInteger.valueOf(memberId));
        return row == null || row.getStatus() == MemberStatus.REMOVED ? null : row;
    }

    /**
     * 在调用方事务中锁定当前租户部门。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentId 部门 ID
     * @return 命中一行时为 true
     */
    public boolean lockDepartment(long tenantId, long departmentId) {
        return departments.lock(BigInteger.valueOf(tenantId), BigInteger.valueOf(departmentId)) != null;
    }

    /**
     * 按读取时的版本条件更新平台显示资料并递增版本，空引用表示保持原值。
     *
     * @param memberId 平台成员 ID
     * @param displayName 显示名称，可空
     * @param avatar 头像，可空
     * @param version 读取时的版本
     * @return 受影响行数；为 0 表示版本已被并发改写或成员已移出
     */
    public int updatePlatform(long memberId, String displayName, String avatar, BigInteger version) {
        return platformMembers.update(Wrappers.<IamPlatformMemberEntity>lambdaUpdate()
                .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamPlatformMemberEntity::getVersion, version)
                .ne(IamPlatformMemberEntity::getStatus, MemberStatus.REMOVED)
                .set(displayName != null, IamPlatformMemberEntity::getDisplayName, displayName)
                .set(avatar != null, IamPlatformMemberEntity::getAvatar, avatar)
                .set(IamPlatformMemberEntity::getVersion, version.add(BigInteger.ONE))
                .set(IamPlatformMemberEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    /**
     * 按读取时的版本条件更新租户显示资料并递增版本；联系方式传入非空引用时允许写成空串。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param displayName 显示名称，可空
     * @param avatar 头像，可空
     * @param phone 手机号，可空引用表示不改
     * @param email 邮箱，可空引用表示不改
     * @param version 读取时的版本
     * @return 受影响行数；为 0 表示版本已被并发改写或成员已移出
     */
    public int updateTenant(long tenantId, long memberId, String displayName, String avatar, String phone,
                            String email, BigInteger version) {
        return tenantMembers.update(Wrappers.<IamTenantMemberEntity>lambdaUpdate()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamTenantMemberEntity::getVersion, version)
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)
                .set(displayName != null, IamTenantMemberEntity::getDisplayName, displayName)
                .set(avatar != null, IamTenantMemberEntity::getAvatar, avatar)
                .set(phone != null, IamTenantMemberEntity::getPhone, phone)
                .set(email != null, IamTenantMemberEntity::getEmail, email)
                .set(IamTenantMemberEntity::getVersion, version.add(BigInteger.ONE))
                .set(IamTenantMemberEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    private LambdaQueryWrapper<IamTenantMemberEntity> tenantFilter(long tenantId, ObjectScope scope, String phone,
                                                                   String email) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)
                .eq(phone != null && !phone.isBlank(), IamTenantMemberEntity::getPhone, phone)
                .eq(email != null && !email.isBlank(), IamTenantMemberEntity::getEmail, email);
        ObjectScopeSql.restrictTenantMembers(wrapper, scope, BigInteger.valueOf(tenantId));
        return wrapper;
    }
}
