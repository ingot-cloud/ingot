package com.ingot.framework.security.core.userdetails;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>Description  : UsernameUri.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/12.</p>
 * <p>Time         : 8:15 PM.</p>
 */
public class UsernameUri {
    private static final String SCHEME = "ingot";
    private static final String HOST = "username";
    private static final String PRINCIPAL = "principal";
    private static final String USER_TYPE = "userType";
    private static final String TENANT = "tenant";
    private static final String DOMAIN = "domain";
    private static final String USER_IDENTITY_TYPE = "userIdentityType";

    private final UriComponents uri;

    public static UsernameUri of(String value, String userType, String grantType, String tenant, String userIdentityType) {
        return new UsernameUri(value, userType, grantType, tenant, null, userIdentityType);
    }

    /**
     * 构造带管理域的登录标识。
     *
     * @param value 主体标识，用户名、手机号或社交唯一码
     * @param userType 用户类型字面量
     * @param grantType 授权类型
     * @param tenant 登录租户，可为空
     * @param domain 认证入口声明的管理域字面量，可为空表示按既有行为兼容
     * @param userIdentityType 身份类型字面量
     * @return 登录标识
     */
    public static UsernameUri of(String value, String userType, String grantType, String tenant,
                                 String domain, String userIdentityType) {
        return new UsernameUri(value, userType, grantType, tenant, domain, userIdentityType);
    }

    public static UsernameUri of(String uriValue) {
        return new UsernameUri(uriValue);
    }

    private UsernameUri(String value, String userType, String grantType, String tenant,
                        String domain, String userIdentityType) {
        this.uri = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(HOST)
                .path(grantType)
                .queryParam(PRINCIPAL, value)
                .queryParam(USER_TYPE, userType)
                .queryParam(TENANT, tenant)
                .queryParam(DOMAIN, domain)
                .queryParam(USER_IDENTITY_TYPE, userIdentityType)
                .build();
    }

    private UsernameUri(String uri) {
        this.uri = UriComponentsBuilder.fromUriString(uri).build();
    }

    public String getValue() {
        return this.uri.toString();
    }

    public String getGrantType() {
        String path = this.uri.getPath();
        if (StrUtil.isEmpty(path)) {
            return null;
        }
        return StrUtil.subAfter(path, "/", false);
    }

    public String getPrincipal() {
        String principal = this.uri.getQueryParams().getFirst(PRINCIPAL);
        return StrUtil.isEmpty(principal) ? this.getValue() : principal;
    }

    public UserTypeEnum getUserType() {
        UserTypeEnum userType = UserTypeEnum.getEnum(this.uri.getQueryParams().getFirst(USER_TYPE));
        return userType == null ? UserTypeEnum.ADMIN : userType;
    }

    public long getTenant() {
        String tenant = this.uri.getQueryParams().getFirst(TENANT);
        return StrUtil.isEmpty(tenant) ? 0 : NumberUtil.parseLong(tenant, 0L);
    }

    /**
     * 读取认证入口声明的管理域。
     *
     * @return 管理域；未声明或字面量非法时返回 {@code null}，由身份提供方按既有行为兼容
     */
    public AuthorizationDomain getDomain() {
        String domain = this.uri.getQueryParams().getFirst(DOMAIN);
        if (StrUtil.isEmpty(domain)) {
            return null;
        }
        try {
            return AuthorizationDomain.getEnum(domain);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public UserIdentityTypeEnum getUserIdentityType() {
        UserIdentityTypeEnum userIdentityType = UserIdentityTypeEnum.getEnum(this.uri.getQueryParams().getFirst(USER_IDENTITY_TYPE));
        return userIdentityType == null ? UserIdentityTypeEnum.USERNAME : userIdentityType;
    }
}
