package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.persistence.mapper.*;
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
     * 读取当前域启用的应用，按排序值和 ID 排序。
     * @param domain 当前授权域
     * @return 应用摘要列表
     */
    public List<ApplicationSummary> applications(AuthorizationDomain domain) {
        return applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getDomain, domain).eq(IamApplicationEntity::getEnabled, true)
                .orderByAsc(IamApplicationEntity::getSortOrder, IamApplicationEntity::getId)).stream()
                .map(app -> new ApplicationSummary(app.getId().toString(), app.getCode(), app.getName(),
                        app.getIcon(), app.getSortOrder())).toList();
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
     * 读取启用菜单，调用方仍需按当前授权快照过滤。
     * @return 按排序值与 ID 排序的菜单
     */
    public List<IamMenuEntity> menus() {
        return menus.selectList(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getEnabled, true)
                .orderByAsc(IamMenuEntity::getSortOrder, IamMenuEntity::getId));
    }

    /**
     * 读取菜单关联的操作编码，保留停用操作的关联匹配语义。
     * @param menuId 菜单 ID
     * @return 菜单匹配需要的操作编码
     */
    public List<String> requiredActionCodes(long menuId) {
        var query = new MPJLambdaWrapper<IamMenuActionEntity>()
                .select(IamActionEntity::getCode)
                .innerJoin(IamActionEntity.class, IamActionEntity::getId, IamMenuActionEntity::getActionId)
                .eq(IamMenuActionEntity::getMenuId, BigInteger.valueOf(menuId));
        return menuActions.selectJoinList(IamActionEntity.class, query).stream()
                .map(IamActionEntity::getCode).toList();
    }
}
