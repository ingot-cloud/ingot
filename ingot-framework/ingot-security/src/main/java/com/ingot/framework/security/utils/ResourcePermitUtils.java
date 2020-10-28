package com.ingot.framework.security.utils;

import com.google.common.collect.Lists;
import com.ingot.framework.security.provider.IngotPermitUrlProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : ResourcePermitUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/5.</p>
 * <p>Time         : 10:47 AM.</p>
 */
@Slf4j
@Component
public class ResourcePermitUtils {
    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Resource
    private IngotPermitUrlProperties ingotPermitUrlProperties;

    /**
     * Resource permit ant patterns
     */
    private List<String> commonResourcePermitAntPatterns(){
        return Lists.newArrayList(
                "/druid/**",
                "/login",
                "/error",
                "/favicon.ico"
        );
    }

    /**
     * User permit ant patterns
     */
    private List<String> commonUserPermitAntPatterns(){
        return Lists.newArrayList("/actuator/**");
    }

    /**
     * Resource permit ant patterns
     */
    public List<String> allResourcePermitAntPatterns(){
        List<String> common = commonResourcePermitAntPatterns();
        List<String> ignoreUrls = ingotPermitUrlProperties.getIgnoreUrls();
        List<String> permit = new ArrayList<>();
        permit.addAll(common);
        permit.addAll(ignoreUrls);
        return permit;
    }

    /**
     * Resource permit
     * @param requestURI check url
     */
    public boolean resourcePermit(String requestURI){
        return allResourcePermitAntPatterns().stream().anyMatch(url -> antPathMatcher.match(url, requestURI));
    }

    /**
     * 是否过滤 {@link com.ingot.framework.security.core.filter.UserAuthenticationFilter}
     * @param requestURI check url
     */
    public boolean userPermit(String requestURI){
        List<String> list = Lists.newArrayList();
        list.addAll(commonResourcePermitAntPatterns());
        list.addAll(commonUserPermitAntPatterns());
        list.addAll(ingotPermitUrlProperties.getIgnoreUserUrls());
        return list.stream().anyMatch(url -> antPathMatcher.match(url, requestURI));
    }
}
