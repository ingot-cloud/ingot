package com.ingot.framework.security.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;

/**
 * <p>Description  : AuthenticationService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-30.</p>
 * <p>Time         : 14:05.</p>
 */
public interface AuthenticationService {

    /**
     * 验证权限
     * @param authentication Authentication
     * @param request 请求
     * @return 是否 grant
     */
    boolean authenticate(Authentication authentication, HttpServletRequest request);
}
