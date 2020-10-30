package com.ingot.framework.security.core.authorize.provider;

import com.ingot.framework.security.config.IngotSecurityFilterConfig;
import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : IgnoreBearerTokenConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/5.</p>
 * <p>Time         : 10:53 AM.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotFilterConfigProvider implements AuthorizeConfigProvider {
    private final IngotSecurityFilterConfig ingotSecurityFilterConfig;

    @Override public boolean config(HttpSecurity http) throws Exception {
        http.apply(ingotSecurityFilterConfig);
        return false;
    }
}
