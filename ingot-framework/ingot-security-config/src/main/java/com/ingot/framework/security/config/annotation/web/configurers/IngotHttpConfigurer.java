package com.ingot.framework.security.config.annotation.web.configurers;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * <p>Description  : IngotHttpConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 3:57 下午.</p>
 */
public class IngotHttpConfigurer
        extends AbstractHttpConfigurer<IngotHttpConfigurer, HttpSecurity> {

    @Override
    public void init(HttpSecurity builder) throws Exception {

    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {

    }
}
