package com.ingot.cloud.iam.infrastructure.persistence;

import java.util.Set;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentView;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>聚合成员持久化读写接口，SQL 和行锁分别委托查询与写入仓储。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberRepository {
    private final MemberQueryRepository queries;
    private final MemberWriteRepository writes;


    /**
     * 分页列出未移出的平台成员。
     *
     * @param scope 已编译范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 成员页
     */
    public Page<IamPlatformMemberEntity> pagePlatform(ObjectScope scope, int page, int size) {
        return queries.pagePlatform(scope, page, size);
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
        return queries.pagePlatform(scope, page, size, name, status);
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
        return queries.pagePlatform(scope, page, size, name, status, ids);
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
        return queries.pagePlatformByGroup(scope, groupId, page, size, name);
    }

    /**
     * 为已通过对象范围校验的平台成员详情读取未删除账号的登录信息白名单。
     *
     * @param accountId 该成员关联的全局账号 ID
     * @return 仅包含账号 ID、登录名、登录手机号与最近登录时间；账号不存在或已删除时为空
     */
    public IamAccountEntity accountLoginInfo(BigInteger accountId) {
        return queries.accountLoginInfo(accountId);
    }

    /**
     * 按账号 ID 读取未删除账号的联系资料。
     *
     * @param accountIds 全局账号 ID
     * @return 以账号 ID 索引的联系资料
     */
    public Map<BigInteger, IamAccountEntity> accountContacts(Collection<BigInteger> accountIds) {
        return queries.accountContacts(accountIds);
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
        return queries.pageTenant(tenantId, scope, phone, email, page, size);
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
        return queries.pageTenant(tenantId, scope, phone, email, departmentId, status, page, size);
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
        return queries.pageTenantCandidates(tenantId, scope, name, ids, page, size);
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
        return queries.listTenantByIds(tenantId, scope, memberIds);
    }

    /**
     * 按全局账号读取平台成员资格，包含暂停与已移出。
     *
     * @param accountId 全局账号 ID
     * @return 成员记录；该账号从未写入平台成员表时为空
     */
    public IamPlatformMemberEntity findPlatformByAccount(long accountId) {
        return queries.findPlatformByAccount(accountId);
    }

    /**
     * 按 ID 读取平台成员，包含已移出。
     *
     * @param memberId 平台成员 ID
     * @return 成员记录，不存在时为空
     */
    public IamPlatformMemberEntity findPlatform(long memberId) {
        return queries.findPlatform(memberId);
    }

    /**
     * 读取当前租户未移出成员。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 成员记录，不存在时为空
     */
    public IamTenantMemberEntity findTenant(long tenantId, long memberId) {
        return queries.findTenant(tenantId, memberId);
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
        return queries.visible(domain, tenantId, memberId, scope);
    }

    /**
     * 按同一平台范围批量检查当前页未移出的成员，不替代关联列表或写入门禁。
     * @param ids 当前页成员 ID
     * @param scope 本操作的范围
     * @return 范围内成员 ID
     */
    public Set<BigInteger> visiblePlatformIds(Collection<BigInteger> ids, ObjectScope scope) {
        return queries.visiblePlatformIds(ids, scope);
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
        return queries.visibleTenantIds(tenantId, ids, scope);
    }

    /** 已绑定资料回显可保留移出记录；仍按租户及完整成员SQL范围过滤。 */
    public Set<BigInteger> visibleTenantIds(long tenantId, Collection<BigInteger> ids, ObjectScope scope, boolean includeRemoved) {
        return queries.visibleTenantIds(tenantId, ids, scope, includeRemoved);
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
        return queries.departmentViews(tenantId, memberIds);
    }

    /**
     * 检查启用且未删除的全局账号是否存在。
     *
     * @param accountId 全局账号 ID
     * @return 恰好一条时为 true
     */
    public boolean activeAccount(long accountId) {
        return queries.activeAccount(accountId);
    }

    /**
     * 判断账号是否仍被未移出的平台或租户成员引用。
     *
     * @param accountId 全局账号 ID
     * @return 仍有成员资格时为 true
     */
    public boolean hasMembership(long accountId) {
        return queries.hasMembership(accountId);
    }

    /**
     * 新增平台成员资格，一次复制账号联系方式为平台联系资料，后续不自动同步。
     *
     * @param id 新成员 ID
     * @param accountId 全局账号 ID
     * @param displayName 显示名称
     * @param avatar 头像引用；入库只保存 {@code bucket/objectName}
     */
    public void insertPlatform(long id, long accountId, String displayName, String avatar) {
        writes.insertPlatform(id, accountId, displayName, avatar);
    }

    /**
     * 新增当前租户成员资格。
     *
     * @param id 新成员 ID
     * @param tenantId 已授权租户 ID
     * @param accountId 全局账号 ID
     * @param displayName 显示名称
     * @param avatar 头像引用；入库只保存 {@code bucket/objectName}
     */
    public void insertTenant(long id, long tenantId, long accountId, String displayName, String avatar) {
        writes.insertTenant(id, tenantId, accountId, displayName, avatar);
    }

    /**
     * 新增当前租户成员资格，并写入组织通讯录联系方式。
     *
     * @param id 新成员 ID
     * @param tenantId 已授权租户 ID
     * @param accountId 全局账号 ID
     * @param displayName 显示名称
     * @param avatar 头像引用；入库只保存 {@code bucket/objectName}
     * @param phone 组织通讯录手机号
     * @param email 组织通讯录邮箱
     */
    public void insertTenant(long id, long tenantId, long accountId, String displayName, String avatar, String phone,
            String email) {
        writes.insertTenant(id, tenantId, accountId, displayName, avatar, phone, email);
    }

    /**
     * 替换当前租户成员任职关系。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param departments 目标任职，键为部门 ID
     */
    public void replaceDepartments(long tenantId, long memberId, Map<String, Boolean> departments) {
        writes.replaceDepartments(tenantId, memberId, departments);
    }

    /**
     * 在调用方事务中锁定平台成员，包含已移出。
     *
     * @param memberId 平台成员 ID
     * @return 锁定行，不存在时为空
     */
    public IamPlatformMemberEntity lockPlatform(long memberId) {
        return writes.lockPlatform(memberId);
    }

    /**
     * 在调用方事务中锁定未移出的租户成员。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 锁定行，不存在或已移出时为空
     */
    public IamTenantMemberEntity lockTenant(long tenantId, long memberId) {
        return writes.lockTenant(tenantId, memberId);
    }

    /**
     * 在调用方事务中锁定租户成员，包含已移出。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 锁定行，不存在时为空
     */
    public IamTenantMemberEntity lockTenantAny(long tenantId, long memberId) {
        return writes.lockTenantAny(tenantId, memberId);
    }

    /**
     * 按账号锁定当前租户成员资格，包含已移出。
     *
     * @param tenantId 已授权租户 ID
     * @param accountId 全局账号 ID
     * @return 锁定行，从未加入时为空
     */
    public IamTenantMemberEntity lockTenantByAccount(long tenantId, long accountId) {
        return writes.lockTenantByAccount(tenantId, accountId);
    }

    /**
     * 将已移出成员改回在职并覆盖通讯录资料。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param displayName 显示名称
     * @param phone 组织通讯录手机号
     * @param email 组织通讯录邮箱，可空
     * @param avatar 已授权的头像路径
     * @param version 锁定时读到的版本
     * @return 受影响行数；为 0 表示版本冲突或资格已不是移出
     */
    public int rejoinTenant(long tenantId, long memberId, String displayName, String phone, String email, String avatar,
            BigInteger version) {
        return writes.rejoinTenant(tenantId, memberId, displayName, phone, email, avatar, version);
    }

    /**
     * 在调用方事务中锁定当前租户部门。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentId 部门 ID
     * @return 命中一行时为 true
     */
    public boolean lockDepartment(long tenantId, long departmentId) {
        return writes.lockDepartment(tenantId, departmentId);
    }

    /**
     * 按成员版本更新平台资料并递增版本，联系资料不修改全局账号；空引用保持原值，空白清空。
     *
     * @param memberId 平台成员 ID
     * @param displayName 显示名称，可空
     * @param avatar 头像引用，可空表示不修改；入库只保存 {@code bucket/objectName}
     * @param phone 平台联系手机号，可空表示不修改，空白清空
     * @param email 平台联系邮箱，可空表示不修改，空白清空
     * @param version 读取时的版本
     * @param suppliedFields 实际提交字段，显式 null 清空 nullable 列
     * @return 受影响行数；为 0 表示版本已被并发改写
     */
    public int updatePlatform(long memberId, String displayName, String avatar, String phone, String email,
            BigInteger version, Set<String> suppliedFields) {
        return writes.updatePlatform(memberId, displayName, avatar, phone, email, version, suppliedFields);
    }

    /**
     * 按读取时的版本条件更新租户显示资料并递增版本；联系方式传入非空引用时允许写成空串。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param displayName 显示名称，可空
     * @param avatar 头像引用，可空表示不修改；入库只保存 {@code bucket/objectName}
     * @param phone 手机号，可空引用表示不改
     * @param email 邮箱，可空引用表示不改
     * @param version 读取时的版本
     * @param suppliedFields 实际提交字段，显式 null 清空 nullable 列
     * @return 受影响行数；为 0 表示版本已被并发改写或成员已移出
     */
    public int updateTenant(long tenantId, long memberId, String displayName, String avatar, String phone,
                            String email, BigInteger version, Set<String> suppliedFields) {
        return writes.updateTenant(tenantId, memberId, displayName, avatar, phone, email, version, suppliedFields);
    }

    /**
     * 按成员列表相同的 SQL 范围统计在册人数，不读取成员资料或任职明细。
     *
     * @param tenantId 当前可信组织 ID
     * @param scope 成员读取范围
     * @return 含暂停、不含已移出的成员数
     */
    public long countTenant(long tenantId, ObjectScope scope) {
        return queries.countTenant(tenantId, scope);
    }
}
