package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.*;
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
    private final IamActionMapper actions;
    private final IamMenuMapper menus;
    private final IamMenuActionMapper menuActions;
    private final IamTenantAppEntitlementMapper entitlements;

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
        List<ApplicationSummary> apps = applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getDomain, identity.domain()).eq(IamApplicationEntity::getEnabled, true)
                .orderByAsc(IamApplicationEntity::getSortOrder, IamApplicationEntity::getId)).stream()
                .map(app -> new ApplicationSummary(app.getId().toString(), app.getCode(), app.getName(),
                        app.getIcon(), app.getSortOrder())).toList();
        if (identity.domain() != AuthorizationDomain.TENANT) {
            return apps;
        }
        BigInteger tenantId = new BigInteger(identity.tenantId());
        BigInteger memberId = new BigInteger(identity.memberId());
        List<ApplicationSummary> visible = new ArrayList<>();
        for (ApplicationSummary app : apps) {
            AuthorizationEvalRows.Entitlement entitlement = entitlements.entitlementForMember(tenantId,
                    new BigInteger(app.id()), memberId, AudienceKind.ALL);
            if (entitlement != null && entitlement.hits() > 0) {
                visible.add(app);
            }
        }
        return visible;
    }

    /**
     * 读取应用启用的操作编码。
     * @param applicationId 应用 ID
     * @return 精确操作编码
     */
    public List<String> enabledActionCodes(long applicationId) {
        return actions.selectList(Wrappers.<IamActionEntity>lambdaQuery().select(IamActionEntity::getCode)
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getEnabled, true)).stream().map(IamActionEntity::getCode).toList();
    }

    /**
     * 读取指定域启用应用下的启用菜单，调用方仍需按开通与 ACTION 过滤。
     *
     * @param domain 当前授权域
     * @return 按排序值与 ID 排序的菜单
     */
    public List<IamMenuEntity> menus(AuthorizationDomain domain) {
        List<BigInteger> applicationIds = applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                        .select(IamApplicationEntity::getId)
                        .eq(IamApplicationEntity::getDomain, domain)
                        .eq(IamApplicationEntity::getEnabled, true)).stream()
                .map(IamApplicationEntity::getId).toList();
        if (applicationIds.isEmpty()) {
            return List.of();
        }
        return menus.selectList(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getEnabled, true)
                .in(IamMenuEntity::getApplicationId, applicationIds)
                .orderByAsc(IamMenuEntity::getSortOrder, IamMenuEntity::getId));
    }

    /**
     * 批量读取菜单关联的操作编码，保留停用操作的关联匹配语义。
     *
     * @param menuIds 菜单 ID
     * @return 菜单到所需操作编码
     */
    public Map<Long, List<String>> requiredActionCodes(Collection<Long> menuIds) {
        Map<Long, List<String>> required = new LinkedHashMap<>();
        if (menuIds == null || menuIds.isEmpty()) {
            return required;
        }
        List<BigInteger> ids = menuIds.stream().map(BigInteger::valueOf).toList();
        List<IamMenuActionEntity> links = menuActions.selectList(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .in(IamMenuActionEntity::getMenuId, ids));
        if (links.isEmpty()) {
            return required;
        }
        Map<BigInteger, String> codes = new LinkedHashMap<>();
        for (IamActionEntity action : actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                .select(IamActionEntity::getId, IamActionEntity::getCode)
                .in(IamActionEntity::getId, links.stream().map(IamMenuActionEntity::getActionId).toList()))) {
            codes.put(action.getId(), action.getCode());
        }
        for (IamMenuActionEntity link : links) {
            String code = codes.get(link.getActionId());
            if (code == null) {
                continue;
            }
            required.computeIfAbsent(link.getMenuId().longValueExact(), key -> new ArrayList<>()).add(code);
        }
        return required;
    }
}
