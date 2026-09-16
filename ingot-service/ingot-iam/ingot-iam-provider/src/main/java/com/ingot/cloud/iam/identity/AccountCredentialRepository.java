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
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>通过 MyBatis Plus 与 MPJ 查询账号凭证和当前域关系，不复用旧用户实体。</p>
 * <p>保留既有登录优先级；锁定事实由安全框架 {@link LockStatePort} 提供，本类不持久化锁定态。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AccountCredentialRepository {
    private final IamAccountMapper accounts;
    private final LockStatePort lockStates;
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

    /**
     * 读取账号当前锁定事实，直接复用安全框架端口。
     *
     * @param accountId 全局账号 ID
     * @return 是否锁定；无锁定记录视为未锁定
     * @throws org.springframework.dao.DataAccessException 锁定态不可读时向上传播，不得降级为未锁定
     */
    public boolean locked(long accountId) {
        return lockStates.findByUser(accountId, UserTypeEnum.ADMIN)
                .map(LockState::isLocked).orElse(false);
    }

    /**
     * 转为既有内部账号 RPC 视图，不返回凭证哈希。
     *
     * @param account 账号凭证事实
     * @param locked 由 {@link #locked(long)} 解析的锁定事实
     * @return 内部账号视图
     */
    public InnerUserDTO toInnerUser(AccountCredentials account, boolean locked) {
        InnerUserDTO dto = new InnerUserDTO();
        dto.setId(account.id());
        dto.setUsername(account.username());
        dto.setNickname(account.username());
        dto.setPhone(account.phone());
        dto.setEmail(account.email());
        dto.setEnabled(account.enabled());
        dto.setLocked(locked);
        return dto;
    }

    private AccountCredentials map(IamAccountEntity row) {
        return new AccountCredentials(row.getId().longValueExact(), row.getUsername(), row.getPasswordHash(),
                row.getPhone(), row.getEmail(),
                Boolean.TRUE.equals(row.getEnabled()), Boolean.TRUE.equals(row.getMustChangePassword()),
                row.getPasswordChangedAt(), row.getLastLoginAt(), row.getVersion().longValueExact(),
                row.getCreatedAt(), row.getUpdatedAt());
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
     * @param passwordChangedAt 最近改密时间
     * @param lastLoginAt 最近登录时间
     * @param version 乐观锁版本
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public record AccountCredentials(long id, String username, String passwordHash, String phone, String email,
                                     boolean enabled, boolean mustChangePassword,
                                     java.time.LocalDateTime passwordChangedAt,
                                     java.time.LocalDateTime lastLoginAt, long version,
                                     java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
    }
}
