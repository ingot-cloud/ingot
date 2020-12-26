package com.ingot.framework.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : SecurityConfigManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 下午12:07.</p>
 */
public interface SecurityConfigManager {

    /**
     * Http security authorize config
     *
     * @param http HttpSecurity
     */
    void config(HttpSecurity http) throws Exception;
}
