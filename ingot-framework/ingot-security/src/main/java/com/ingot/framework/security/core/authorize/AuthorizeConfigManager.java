package com.ingot.framework.security.core.authorize;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : AuthorizeManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:07.</p>
 */
public interface AuthorizeConfigManager {

    /**
     * Http security authorize config
     *
     * @param http HttpSecurity
     */
    void config(HttpSecurity http) throws Exception;
}
