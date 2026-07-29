package com.ingot.cloud.member.identity;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.ingot.cloud.member.api.model.convert.MemberUserConvert;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.tenant.properties.TenantProperties;

/**
 * <p>Description  : IdentityUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:52.</p>
 */
public class IdentityUtil {
    /**
     * 映射用户信息
     */
    public static UserDetailsResponse map(MemberUser user,
                                          UserTypeEnum userType,
                                          BizUserService bizUserService,
                                          TenantProperties tenantProperties) {
        // App用户简单处理，暂时不走复杂逻辑

        // 账号维度：来自 member_user.enabled / member_user.locked
        boolean userEnabled = Boolean.TRUE.equals(user.getEnabled());
        boolean userLocked = Boolean.TRUE.equals(user.getLocked());

        UserDetailsResponse result = MemberUserConvert.INSTANCE.toUserDetails(user);
        result.setTenant(tenantProperties.getDefaultId());
        result.setUserType(userType.getValue());
        result.setEnabled(userEnabled);
        result.setLocked(userLocked);

        // 账号不可用则无需查询 scope
        if (!userEnabled || userLocked) {
            return result;
        }

        List<String> scopes = new ArrayList<>();
        // 强制修改密码：仅下发初始密码修改权限
        if (BooleanUtil.isTrue(user.getMustChangePwd())) {
            scopes.add(PermissionConstants.INIT_PASSWORD);
            result.setScopes(scopes);
            return result;
        }

        scopes.addAll(getScopes(user, bizUserService));
        result.setScopes(scopes);
        return result;
    }

    private static List<String> getScopes(MemberUser user,
                                          BizUserService bizUserService) {
        List<MemberRole> roles = bizUserService.getUserRoles(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return roles.stream().map(MemberRole::getCode).toList();
    }

}
