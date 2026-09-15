package com.ingot.cloud.iam.identity;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.api.model.dto.user.InnerUserDTO;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

/**
 * <p>通过 MyBatis Plus 与 MPJ 查询账号凭证和当前域关系，不复用旧用户实体。</p>
 * <p>保留既有登录优先级与锁定查询行为；认证是否回退由上层编排决定。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AccountCredentialRepository {
    private final IamAccountMapper accounts;
    private final IamAccountLockStateMapper locks;
    private final IamTenantMemberMapper members;
    private final IamMemberDepartmentMapper departments;

    /** 先按手机号再按用户名读取未删除账号，不为查询附加任何成员权限。 */
    public Optional<AccountCredentials> findByLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        var phone = accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getPhone, login).isNull(IamAccountEntity::getDeletedAt));
        if (!phone.isEmpty()) {
            return Optional.of(map(phone.getFirst()));
        }
        return accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getUsername, login).isNull(IamAccountEntity::getDeletedAt))
                .stream().findFirst().map(this::map);
    }

    /** 按全局账号 ID 读取未删除的账号；不包含租户成员资料。 */
    public Optional<AccountCredentials> findById(long accountId) {
        return accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getId, accountId).isNull(IamAccountEntity::getDeletedAt))
                .stream().findFirst().map(this::map);
    }

    /** 列出指定账号有效的租户成员组织；账号来自认证流程，不含平台身份。 */
    public List<TenantMainDTO> tenantAllows(String accountId) {
        var query = new MPJLambdaWrapper<IamTenantMemberEntity>()
                .selectAs(IamTenantEntity::getId, TenantMainDTO::getId)
                .selectAs(IamTenantEntity::getName, TenantMainDTO::getName)
                .innerJoin(IamTenantEntity.class, IamTenantEntity::getId, IamTenantMemberEntity::getTenantId)
                .eq(IamTenantMemberEntity::getAccountId, Long.parseLong(accountId))
                .eq(IamTenantMemberEntity::getStatus, MemberStatus.ACTIVE)
                .eq(IamTenantEntity::getEnabled, true).isNull(IamTenantEntity::getDeletedAt)
                .orderByAsc(IamTenantEntity::getId);
        List<TenantMainDTO> result = members.selectJoinList(TenantMainDTO.class, query);
        result.forEach(tenant -> tenant.setMain(false));
        return result;
    }

    /** 读取当前租户成员的任职关系，显式限定 tenantId 与 memberId。 */
    public List<Long> departments(String tenantId, String memberId) {
        return departments.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .select(IamMemberDepartmentEntity::getDepartmentId)
                .eq(IamMemberDepartmentEntity::getTenantId, Long.parseLong(tenantId))
                .eq(IamMemberDepartmentEntity::getMemberId, Long.parseLong(memberId))
                .orderByDesc(IamMemberDepartmentEntity::getIsPrimary)
                .orderByAsc(IamMemberDepartmentEntity::getDepartmentId))
                .stream().map(row -> row.getDepartmentId().longValueExact()).toList();
    }

    /** 转为既有内部账号 RPC 视图，不返回凭证哈希。 */
    public InnerUserDTO toInnerUser(AccountCredentials account) {
        InnerUserDTO dto = new InnerUserDTO();
        dto.setId(account.id());
        dto.setUsername(account.username());
        dto.setNickname(account.username());
        dto.setPhone(account.phone());
        dto.setEmail(account.email());
        dto.setEnabled(account.enabled());
        dto.setLocked(account.locked());
        return dto;
    }

    private AccountCredentials map(IamAccountEntity row) {
        long id = row.getId().longValueExact();
        return new AccountCredentials(id, row.getUsername(), row.getPasswordHash(), row.getPhone(), row.getEmail(),
                Boolean.TRUE.equals(row.getEnabled()), Boolean.TRUE.equals(row.getMustChangePassword()), locked(id),
                row.getPasswordChangedAt(), row.getLastLoginAt(), row.getVersion().longValueExact(),
                row.getCreatedAt(), row.getUpdatedAt());
    }

    private boolean locked(long accountId) {
        try {
            return locks.selectList(Wrappers.<IamAccountLockStateEntity>lambdaQuery()
                    .select(IamAccountLockStateEntity::getLocked)
                    .eq(IamAccountLockStateEntity::getUserId, accountId)
                    .eq(IamAccountLockStateEntity::getUserType, UserTypeEnum.ADMIN.getValue()))
                    .stream().findFirst().map(row -> Boolean.TRUE.equals(row.getLocked())).orElse(false);
        } catch (DataAccessException exception) {
            // 保留当前锁定辅助表兼容行为；本次仅迁移持久化，不调整认证回退规则。
            return false;
        }
    }

    /**
     * <p>保存登录所需的账号安全字段，不包含成员身份。</p>
     *
     * @author jy
     * @since 1.0.0
     * @param id 账号 ID
     * @param username 登录名
     * @param passwordHash 凭证哈希
     * @param phone 手机号，可空
     * @param email 邮箱，可空
     * @param enabled 全局启用
     * @param mustChangePassword 是否必须改密
     * @param locked 账号锁定，来自辅助锁定表
     * @param passwordChangedAt 最近改密时间
     * @param lastLoginAt 最近登录时间
     * @param version 乐观锁版本
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public record AccountCredentials(long id, String username, String passwordHash, String phone, String email,
                                     boolean enabled, boolean mustChangePassword, boolean locked,
                                     java.time.LocalDateTime passwordChangedAt,
                                     java.time.LocalDateTime lastLoginAt, long version,
                                     java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
    }
}
