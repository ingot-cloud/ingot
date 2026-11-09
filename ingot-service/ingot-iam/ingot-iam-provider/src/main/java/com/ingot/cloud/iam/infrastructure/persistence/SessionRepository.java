package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.domain.account.CurrentProfile;
import com.ingot.cloud.iam.domain.catalog.ApplicationSummary;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.projection.SessionMenuRow;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>读取会话所需的成员资料与应用菜单目录，授权过滤由会话服务执行。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class SessionRepository {
    private final IamPlatformMemberMapper platformMembers;
    private final IamTenantMemberMapper tenantMembers;
    private final IamApplicationMapper applications;
    private final IamMenuMapper menus;
    private final EntitlementRepository entitlements;

    /**
     * 读取当前有效身份的成员资料。
     * @param identity 已通过身份校验的上下文
     * @return 成员资料，不存在时为空
     */
    public CurrentProfile profile(AuthorizationContext identity) {
        BigInteger memberId = new BigInteger(identity.memberId());
        if (identity.domain() == AuthorizationDomain.PLATFORM) {
            var member = platformMembers.selectOne(Wrappers.<IamPlatformMemberEntity>lambdaQuery()
                    .eq(IamPlatformMemberEntity::getId, memberId)
                    .eq(IamPlatformMemberEntity::getStatus, MemberStatus.ACTIVE));
            return member == null ? null : new CurrentProfile(member.getId().toString(),
                    member.getDisplayName(), member.getAvatar());
        }
        var member = tenantMembers.selectOne(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, new BigInteger(identity.tenantId()))
                .eq(IamTenantMemberEntity::getId, memberId)
                .eq(IamTenantMemberEntity::getStatus, MemberStatus.ACTIVE));
        return member == null ? null : new CurrentProfile(member.getId().toString(),
                member.getDisplayName(), member.getAvatar());
    }

    /**
     * 读取当前身份可访问的启用应用：先域与启停，租户再核对开通与人群。
     *
     * @param identity 已通过身份校验的上下文
     * @return 应用摘要列表
     */
    public List<ApplicationSummary> accessibleApplications(AuthorizationContext identity) {
        List<IamApplicationEntity> apps = applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getDomain, identity.domain()).eq(IamApplicationEntity::getEnabled, true)
                .orderByAsc(IamApplicationEntity::getSortOrder, IamApplicationEntity::getId));
        if (identity.domain() == AuthorizationDomain.TENANT) {
            var matches = entitlements.memberEntitlements(new BigInteger(identity.tenantId()),
                    new BigInteger(identity.memberId()), apps.stream().map(IamApplicationEntity::getId).toList());
            apps = apps.stream().filter(app -> matches.containsKey(app.getId())).toList();
        }
        return apps.stream().map(app -> new ApplicationSummary(app.getId().toString(), app.getCode(),
                app.getName(), app.getIcon(), app.getSortOrder())).toList();
    }

    /**
     * 读取当前可访问应用的启用菜单及其操作绑定。
     *
     * @param domain 当前授权域
     * @param applicationIds 已通过访问检查的应用 ID
     * @return 菜单和绑定行
     */
    public List<SessionMenuRow> menus(AuthorizationDomain domain, Collection<Long> applicationIds) {
        return applicationIds.isEmpty() ? List.of() : menus.selectForSession(domain, applicationIds);
    }
}
