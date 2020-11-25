package com.ingot.framework.security.core.authorize.provider;

import com.ingot.framework.security.provider.IngotSecurityFilterConfig;
import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import com.ingot.framework.security.provider.filter.UserAuthenticationFilter;
import com.ingot.framework.security.service.ResourcePermitService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : SecurityFilterConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/5.</p>
 * <p>Time         : 10:53 AM.</p>
 */
@Slf4j
@AllArgsConstructor
public class SecurityFilterConfigProvider implements AuthorizeConfigProvider {
    private final ResourcePermitService resourcePermitService;
    private final UserAuthenticationFilter userAuthenticationFilter;

    @Override public boolean config(HttpSecurity http) throws Exception {
        IngotSecurityFilterConfig ingotSecurityFilterConfig =
                new IngotSecurityFilterConfig(resourcePermitService, userAuthenticationFilter);
        http.apply(ingotSecurityFilterConfig);
        return false;
    }
}
