package com.ingot.framework.security.core.userdetails;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
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

    private final UriComponents uri;

    public static UsernameUri of(String value, String userType, String grantType, String tenant) {
        return new UsernameUri(value, userType, grantType, tenant);
    }

    public static UsernameUri of(String uriValue) {
        return new UsernameUri(uriValue);
    }

    private UsernameUri(String value, String userType, String grantType, String tenant) {
        this.uri = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(HOST)
                .path(grantType)
                .queryParam(PRINCIPAL, value)
                .queryParam(USER_TYPE, userType)
                .queryParam(TENANT, tenant)
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
}
