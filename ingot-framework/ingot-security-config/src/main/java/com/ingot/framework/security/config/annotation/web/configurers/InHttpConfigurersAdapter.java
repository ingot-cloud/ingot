package com.ingot.framework.security.config.annotation.web.configurers;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : InHttpConfigurersAdapter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 5:37 下午.</p>
 */
public interface InHttpConfigurersAdapter {

    /**
     * Http security  config
     *
     * @param http {@link HttpSecurity}
     */
    void apply(HttpSecurity http) throws Exception;
}
