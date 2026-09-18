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

/**
 * <p>把会员账号映射为认证 {@link UserDetailsResponse}。</p>
 *
 * <p>租户字段保留 Convert 映射结果；会员账号本身没有租户时保持 {@code null}，不再填入默认租户 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class IdentityUtil {
    /**
     * 将会员账号转为登录所需的用户详情，并按账号状态填充权限。
     *
     * @param user          已查询到的会员账号
     * @param userType      请求中的用户类型
     * @param bizUserService 用于读取角色权限
     * @return 认证用户详情；账号不可用或需改密时提前返回
     */
    public static UserDetailsResponse map(MemberUser user,
                                          UserTypeEnum userType,
                                          BizUserService bizUserService) {
        // App用户简单处理，暂时不走复杂逻辑

        // 账号维度：来自 member_user.enabled / member_user.locked
        boolean userEnabled = Boolean.TRUE.equals(user.getEnabled());
        boolean userLocked = Boolean.TRUE.equals(user.getLocked());

        UserDetailsResponse result = MemberUserConvert.INSTANCE.toUserDetails(user);
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
