package com.ingot.framework.security.core.authorize.provider;

import com.ingot.framework.security.core.authorize.AuthorizeConfigProvider;
import com.ingot.framework.security.utils.ResourcePermitUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>Description  : AuthorizePermitConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/19.</p>
 * <p>Time         : 上午9:46.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizePermitConfigProvider implements AuthorizeConfigProvider {

    @Value("${spring.application.name}")
    private String applicationName;
    @Resource
    private ResourcePermitUtils resourcePermitUtils;

    @Override public boolean config(HttpSecurity http) throws Exception{
        List<String> permit = resourcePermitUtils.allResourcePermitAntPatterns();
        log.info(">>> {} AuthorizePermitConfig [configure] ========>>> http permit: {}", applicationName, permit);

        if (!permit.isEmpty()){
            http.authorizeRequests().antMatchers(permit.toArray(new String[0])).permitAll();
        }

        return false;
    }
}