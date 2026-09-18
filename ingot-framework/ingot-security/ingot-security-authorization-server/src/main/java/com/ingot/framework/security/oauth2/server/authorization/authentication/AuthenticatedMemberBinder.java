package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.core.userdetails.UsernameUri;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

/**
 * <p>把预授权用户解析成可签发会话的单一成员，租户管理用户必须重新加载成员上下文。</p>
 *
 * <p>选择阶段的预授权用户没有 {@code AuthorizationContext}。签发前按选定租户走身份服务，
 * 禁止管理用户沿用旧预授权部门切片。非管理用户仍按既有租户切片协议处理。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class AuthenticatedMemberBinder {

    private static final String MEMBER_CONTEXT_REQUIRED =
            "IAM identity selection requires a new authenticated member context.";

    private final OAuth2UserDetailsServiceManager users;

    /**
     * 解析本次签发使用的用户：已绑定且租户匹配则原样返回；管理用户选定租户时重载成员。
     *
     * @param user 预授权或已绑定的认证用户
     * @param selectedTenant 授权请求选定的租户；平台签发应为 {@code null}
     * @return 可写入在线会话的用户
     * @throws OAuth2AuthenticationException 已绑定身份试图换租户，或管理用户无法建立成员上下文
     */
    public InUser resolveForIssuance(InUser user, Long selectedTenant) {
        if (user.getAuthorizationContext() != null) {
            if (selectedTenant != null && !Objects.equals(user.getTenantId(), selectedTenant)) {
                throw invalidGrant();
            }
            return user;
        }
        if (isAdmin(user) && selectedTenant != null) {
            return bindSelectedTenant(user, selectedTenant.toString());
        }
        if (selectedTenant == null) {
            return user;
        }
        List<Long> pickedDeptIds = Optional.ofNullable(user.getTenantDeptIds())
                .map(mapping -> mapping.get(selectedTenant))
                .orElseGet(user::getDeptIds);
        return user.toBuilder()
                .tenantId(selectedTenant)
                .deptIds(pickedDeptIds)
                .tenantDeptIds(null)
                .build();
    }

    /**
     * 按选定租户重新加载管理成员，保留当前客户端绑定。
     *
     * @param current 凭证已通过、尚未绑定成员的预授权用户
     * @param tenantId 候选列表中已校验的租户 ID
     * @return 带成员上下文的用户
     * @throws OAuth2AuthenticationException 身份服务失败、账号不一致或未建立成员上下文
     */
    public InUser bindSelectedTenant(InUser current, String tenantId) {
        if (current == null || current.getId() == null || StrUtil.isBlank(tenantId) || users == null) {
            throw invalidGrant();
        }
        UsernameUri uri = UsernameUri.of(
                current.getUsername(),
                current.getUserType(),
                InAuthorizationGrantType.PASSWORD.getValue(),
                tenantId,
                AuthorizationDomain.TENANT.getValue(),
                UserIdentityTypeEnum.USERNAME.getValue());
        UserDetails loaded;
        try {
            loaded = users.loadUser(OAuth2UserDetailsAuthenticationToken.unauthenticated(
                    uri.getValue(), "N/A", InAuthorizationGrantType.PASSWORD, null));
        } catch (RuntimeException exception) {
            throw invalidGrant(exception);
        }
        if (!(loaded instanceof InUser bound)
                || !Objects.equals(current.getId(), bound.getId())
                || bound.getAuthorizationContext() == null
                || !bound.isEnabled()
                || !bound.isAccountNonLocked()) {
            throw invalidGrant();
        }
        try {
            return bound.toBuilder()
                    .clientId(current.getClientId())
                    .tokenAuthType(current.getTokenAuthType())
                    .build();
        } catch (IllegalArgumentException exception) {
            throw invalidGrant(exception);
        }
    }

    private static boolean isAdmin(InUser user) {
        return UserTypeEnum.getEnum(user.getUserType()) == UserTypeEnum.ADMIN;
    }

    private static OAuth2AuthenticationException invalidGrant() {
        return new OAuth2AuthenticationException(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_GRANT, MEMBER_CONTEXT_REQUIRED, null));
    }

    private static OAuth2AuthenticationException invalidGrant(Throwable cause) {
        return new OAuth2AuthenticationException(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_GRANT, MEMBER_CONTEXT_REQUIRED, null), cause);
    }
}
