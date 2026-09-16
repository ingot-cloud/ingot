package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>按当前域锁定并更新成员资格与任职关系，租户操作始终绑定可信 tenantId。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberLifecycleRepository {
    private final IamTenantMapper tenants;
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamDepartmentMapper departments;
    private final IamMemberDepartmentMapper memberDepartments;

    /**
     * 锁定组织并读取所有者成员 ID。
     * @param tenantId 已授权租户 ID
     * @return 所有者成员 ID 文本，组织不存在时为空
     */
    public String lockOwner(long tenantId) {
        IamTenantEntity tenant = tenants.lock(BigInteger.valueOf(tenantId));
        return tenant == null || tenant.getOwnerMemberId() == null ? null : tenant.getOwnerMemberId().toString();
    }

    /**
     * 锁定当前域成员资格。
     * @param domain 当前授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param memberId 成员 ID
     * @return 锁定后的资格与版本，不存在时为空
     */
    public LockedMember lockMember(AuthorizationDomain domain, Long tenantId, long memberId) {
        if (domain == AuthorizationDomain.PLATFORM) {
            IamPlatformMemberEntity member = platformMembers.lock(BigInteger.valueOf(memberId));
            return member == null ? null : new LockedMember(member.getStatus(), member.getVersion());
        }
        IamTenantMemberEntity member = tenantMembers.lock(BigInteger.valueOf(tenantId), BigInteger.valueOf(memberId));
        return member == null ? null : new LockedMember(member.getStatus(), member.getVersion());
    }

    /**
     * 锁定并读取租户成员任职关系。
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 部门 ID 到是否主部门
     */
    public Map<String, Boolean> lockDepartments(long tenantId, long memberId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (IamMemberDepartmentEntity row : memberDepartments.lockByMember(BigInteger.valueOf(tenantId),
                BigInteger.valueOf(memberId))) {
            result.put(row.getDepartmentId().toString(), Boolean.TRUE.equals(row.getIsPrimary()));
        }
        return result;
    }

    /**
     * 锁定当前租户部门。
     * @param tenantId 已授权租户 ID
     * @param departmentId 部门 ID
     * @return 是否恰好命中一行
     */
    public boolean lockDepartment(long tenantId, long departmentId) {
        return departments.lock(BigInteger.valueOf(tenantId), BigInteger.valueOf(departmentId)) != null;
    }

    /**
     * 按持锁读到的版本条件更新当前域成员资格并递增版本。
     * @param domain 当前授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param memberId 成员 ID
     * @param status 新资格
     * @param version 持锁读到的版本
     * @return 受影响行数；为 0 表示版本已被并发改写
     */
    public int updateStatus(AuthorizationDomain domain, Long tenantId, long memberId, MemberStatus status,
                            BigInteger version) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BigInteger next = version.add(BigInteger.ONE);
        if (domain == AuthorizationDomain.PLATFORM) {
            return platformMembers.update(Wrappers.<IamPlatformMemberEntity>lambdaUpdate()
                    .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId))
                    .eq(IamPlatformMemberEntity::getVersion, version)
                    .set(IamPlatformMemberEntity::getStatus, status)
                    .set(IamPlatformMemberEntity::getVersion, next)
                    .set(IamPlatformMemberEntity::getUpdatedAt, now));
        }
        return tenantMembers.update(Wrappers.<IamTenantMemberEntity>lambdaUpdate()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamTenantMemberEntity::getVersion, version)
                .set(IamTenantMemberEntity::getStatus, status)
                .set(IamTenantMemberEntity::getVersion, next)
                .set(IamTenantMemberEntity::getUpdatedAt, now));
    }

    /**
     * 按持锁读到的版本条件仅递增当前域成员版本。
     * @param domain 当前授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param memberId 成员 ID
     * @param version 持锁读到的版本
     * @return 受影响行数；为 0 表示版本已被并发改写
     */
    public int incrementVersion(AuthorizationDomain domain, Long tenantId, long memberId, BigInteger version) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BigInteger next = version.add(BigInteger.ONE);
        if (domain == AuthorizationDomain.PLATFORM) {
            return platformMembers.update(Wrappers.<IamPlatformMemberEntity>lambdaUpdate()
                    .eq(IamPlatformMemberEntity::getId, BigInteger.valueOf(memberId))
                    .eq(IamPlatformMemberEntity::getVersion, version)
                    .set(IamPlatformMemberEntity::getVersion, next)
                    .set(IamPlatformMemberEntity::getUpdatedAt, now));
        }
        return tenantMembers.update(Wrappers.<IamTenantMemberEntity>lambdaUpdate()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId))
                .eq(IamTenantMemberEntity::getVersion, version)
                .set(IamTenantMemberEntity::getVersion, next)
                .set(IamTenantMemberEntity::getUpdatedAt, now));
    }

    /**
     * 替换当前租户成员任职关系。
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param departments 目标任职
     */
    public void replaceDepartments(long tenantId, long memberId, Map<String, Boolean> departments) {
        memberDepartments.delete(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberDepartmentEntity::getMemberId, BigInteger.valueOf(memberId)));
        departments.forEach((departmentId, primary) -> {
            IamMemberDepartmentEntity row = new IamMemberDepartmentEntity();
            row.setTenantId(BigInteger.valueOf(tenantId));
            row.setMemberId(BigInteger.valueOf(memberId));
            row.setDepartmentId(new BigInteger(departmentId));
            row.setIsPrimary(Boolean.TRUE.equals(primary));
            memberDepartments.insert(row);
        });
    }

    /**
     * 读取当前租户成员的安全投影字段，不含联系方式。
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 成员记录，不存在时为空
     */
    public IamTenantMemberEntity findTenantMember(long tenantId, long memberId) {
        return tenantMembers.selectOne(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .select(IamTenantMemberEntity::getId, IamTenantMemberEntity::getDisplayName,
                        IamTenantMemberEntity::getStatus)
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamTenantMemberEntity::getId, BigInteger.valueOf(memberId)));
    }

    /**
     * 读取当前租户成员任职及部门名称。
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 按主部门优先、部门 ID 排序的任职
     */
    public List<MemberDepartmentView> departmentViews(long tenantId, long memberId) {
        BigInteger tenant = BigInteger.valueOf(tenantId);
        return memberDepartments.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .eq(IamMemberDepartmentEntity::getTenantId, tenant)
                        .eq(IamMemberDepartmentEntity::getMemberId, BigInteger.valueOf(memberId))).stream()
                .sorted(Comparator.comparing((IamMemberDepartmentEntity row) -> !Boolean.TRUE.equals(row.getIsPrimary()))
                        .thenComparing(IamMemberDepartmentEntity::getDepartmentId))
                .map(row -> {
                    IamDepartmentEntity department = departments.selectOne(Wrappers.<IamDepartmentEntity>lambdaQuery()
                            .select(IamDepartmentEntity::getId, IamDepartmentEntity::getName)
                            .eq(IamDepartmentEntity::getTenantId, tenant)
                            .eq(IamDepartmentEntity::getId, row.getDepartmentId()));
                    return new MemberDepartmentView(department.getId().toString(), department.getName(),
                            Boolean.TRUE.equals(row.getIsPrimary()));
                }).toList();
    }

    /**
     * <p>保存写锁下读取的成员资格和乐观锁版本。</p>
     * @author jy
     * @since 1.0.0
     * @param status 当前资格
     * @param version 当前版本
     */
    public record LockedMember(MemberStatus status, BigInteger version) {
    }
}
