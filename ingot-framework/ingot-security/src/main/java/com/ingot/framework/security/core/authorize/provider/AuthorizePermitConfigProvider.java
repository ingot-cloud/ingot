package com.ingot.framework.security.core.authorize.provider;

import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import com.ingot.framework.security.service.ResourcePermitService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

/**
 * <p>Description  : AuthorizePermitConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/19.</p>
 * <p>Time         : 上午9:46.</p>
 */
@Slf4j
@AllArgsConstructor
public class AuthorizePermitConfigProvider implements AuthorizeConfigProvider {
    private final String applicationName;
    private final ResourcePermitService resourcePermitService;

    @Override public boolean config(HttpSecurity http) throws Exception{
        List<String> permit = resourcePermitService.allResourcePermitAntPatterns();
        log.info(">>> {} AuthorizePermitConfig [configure] ========>>> http permit: {}", applicationName, permit);

        if (!permit.isEmpty()){
            http.authorizeRequests().antMatchers(permit.toArray(new String[0])).permitAll();
        }

        return false;
    }
}