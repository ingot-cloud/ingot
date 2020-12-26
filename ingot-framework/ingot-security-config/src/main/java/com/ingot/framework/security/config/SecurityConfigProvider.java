package com.ingot.framework.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : AuthorizeConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:02.</p>
 */
public interface SecurityConfigProvider {

    /**
     * Http security authorize config provider
     *
     * @param http HttpSecurity
     * @return true indicates that anyRequest has been configured,
     */
    boolean config(HttpSecurity http) throws Exception;
}
