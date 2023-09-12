package com.ingot.framework.security.core.userdetails;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.common.constants.UserType;
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

    private final UriComponents uri;

    public static UsernameUri of(String value, String userType, String grantType) {
        return new UsernameUri(value, userType, grantType);
    }

    public static UsernameUri of(String uriValue) {
        return new UsernameUri(uriValue);
    }

    private UsernameUri(String value, String userType, String grantType) {
        this.uri = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(HOST)
                .path(grantType)
                .queryParam(PRINCIPAL, value)
                .queryParam(USER_TYPE, userType)
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

    public UserType getUserType() {
        UserType userType = UserType.getEnum(this.uri.getQueryParams().getFirst(USER_TYPE));
        return userType == null ? UserType.ADMIN : userType;
    }
}
