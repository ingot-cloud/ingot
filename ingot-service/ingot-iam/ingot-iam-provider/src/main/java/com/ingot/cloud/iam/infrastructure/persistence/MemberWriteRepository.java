package com.ingot.cloud.iam.infrastructure.persistence;

import java.util.Set;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.infrastructure.storage.IamOssPaths;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>维护成员资格、资料与任职，并在原事务内取得成员和部门行锁。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberWriteRepository {
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamDepartmentMapper departments;
    private final MemberDepartmentWriteRepository departmentWrites;
    private final MemberQueryRepository queries;


    /**
     * 新增平台成员资格，一次复制账号联系方式为平台联系资料，后续不自动同步。
     *
     * @param id 新成员 ID
     * @param accountId 全局账号 ID
     * @param displayName 显示名称
     * @param avatar 头像引用；入库只保存 {@code bucket/objectName}
     */
    public void insertPlatform(long id, long accountId, String displayName, String avatar) {
        IamPlatformMemberEntity row = new IamPlatformMemberEntity();
        row.setId(BigInteger.valueOf(id));
        row.setAccountId(BigInteger.valueOf(accountId));
        row.setDisplayName(displayName);
        row.setAvatar(IamOssPaths.store(avatar));
        IamAccountEntity account = queries.accountContacts(List.of(row.getAccountId())).get(row.getAccountId());
        row.setPhone(account == null ? null : account.getPhone());
        row.setEmail(account == null ? null : account.getEmail());
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
     * @param avatar 头像引用；入库只保存 {@code bucket/objectName}
     */
    public void insertTenant(long id, long tenantId, long accountId, String displayName, String avatar) {
        insertTenant(id, tenantId, accountId, displayName, avatar, null, null);
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
        IamTenantMemberEntity row = new IamTenantMemberEntity();
        row.setId(BigInteger.valueOf(id));
        row.setTenantId(BigInteger.valueOf(tenantId));
        row.setAccountId(BigInteger.valueOf(accountId));
        row.setDisplayName(displayName);
        row.setAvatar(IamOssPaths.store(avatar));
        row.setPhone(phone);
        row.setEmail(email);
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
        departmentWrites.replace(tenantId, memberId, departments);
    }

    /**
     * 在调用方事务中锁定平台成员，包含已移出。
     *
     * @param memberId 平台成员 ID
     * @return 锁定行，不存在时为空
     */
    public IamPlatformMemberEntity lockPlatform(long memberId) {
        return platformMembers.lock(BigInteger.valueOf(memberId));
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
     * 在调用方事务中锁定租户成员，包含已移出。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 锁定行，不存在时为空
     */
    public IamTenantMemberEntity lockTenantAny(long tenantId, long memberId) {
        return tenantMembers.lockAny(BigInteger.valueOf(tenantId), BigInteger.valueOf(memberId));
    }

    /**
     * 按账号锁定当前租户成员资格，包含已移出。
     *
     * @param tenantId 已授权租户 ID
     * @param accountId 全局账号 ID
     * @return 锁定行，从未加入时为空
     */
    public IamTenantMemberEntity lockTenantByAccount(long tenantId, long accountId) {
        return tenantMembers.lockByAccount(BigInteger.valueOf(tenantId), BigInteger.valueOf(accountId));
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
        return tenantMembers.update(Wrappers.<IamTenantMemberEntity>lambdaUpdate()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamTenantMemberEntity::getVersion, version)
                .eq(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)
                .set(IamTenantMemberEntity::getDisplayName, displayName)
                .set(IamTenantMemberEntity::getAvatar, avatar)
                .set(IamTenantMemberEntity::getPhone, phone)
                .set(IamTenantMemberEntity::getEmail, email)
                .set(IamTenantMemberEntity::getStatus, MemberStatus.ACTIVE)
                .set(IamTenantMemberEntity::getVersion, version.add(BigInteger.ONE))
                .set(IamTenantMemberEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
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
        return platformMembers.update(Wrappers.<IamPlatformMemberEntity>lambdaUpdate()
                .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamPlatformMemberEntity::getVersion, version)
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_DISPLAY_NAME), IamPlatformMemberEntity::getDisplayName, displayName)
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_AVATAR), IamPlatformMemberEntity::getAvatar, IamOssPaths.store(avatar))
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_PHONE), IamPlatformMemberEntity::getPhone, phone == null || phone.isBlank() ? null : phone)
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_EMAIL), IamPlatformMemberEntity::getEmail, email == null || email.isBlank() ? null : email)
                .set(IamPlatformMemberEntity::getVersion, version.add(BigInteger.ONE))
                .set(IamPlatformMemberEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
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
        return tenantMembers.update(Wrappers.<IamTenantMemberEntity>lambdaUpdate()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamTenantMemberEntity::getVersion, version)
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_DISPLAY_NAME), IamTenantMemberEntity::getDisplayName, displayName)
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_AVATAR), IamTenantMemberEntity::getAvatar, IamOssPaths.store(avatar))
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_PHONE), IamTenantMemberEntity::getPhone, phone == null || phone.isBlank() ? null : phone)
                .set(suppliedFields.contains(com.ingot.cloud.iam.domain.policy.MemberFieldKey.VALUE_EMAIL), IamTenantMemberEntity::getEmail, email == null || email.isBlank() ? null : email)
                .set(IamTenantMemberEntity::getVersion, version.add(BigInteger.ONE))
                .set(IamTenantMemberEntity::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
    }
}
