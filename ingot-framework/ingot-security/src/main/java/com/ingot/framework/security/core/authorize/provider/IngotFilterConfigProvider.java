package com.ingot.framework.security.core.authorize.provider;

import com.ingot.framework.security.config.IngotSecurityFilterConfig;
import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * <p>Description  : IgnoreBearerTokenConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/5.</p>
 * <p>Time         : 10:53 AM.</p>
 */
@Slf4j
@Component
public class IngotFilterConfigProvider implements AuthorizeConfigProvider {

    @Resource
    private IngotSecurityFilterConfig ingotSecurityFilterConfig;

    @Override public boolean config(HttpSecurity http) throws Exception {
        http.apply(ingotSecurityFilterConfig);
        return false;
    }
}
