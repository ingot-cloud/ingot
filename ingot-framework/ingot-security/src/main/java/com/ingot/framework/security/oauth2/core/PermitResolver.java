package com.ingot.framework.security.oauth2.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Description  : PermitResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/22.</p>
 * <p>Time         : 5:00 下午.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class PermitResolver implements InitializingBean {
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");
    private static final String VERTICAL_LINE = "|";
    private static final String[] DEFAULT_IGNORE_URLS = new String[]{"/actuator/**", "/error", "/v3/api-docs"};

    private final WebApplicationContext applicationContext;
    private final InOAuth2ResourceProperties properties;

    /**
     * permit all public url
     */
    public void permitAllPublic(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        List<String> urls = properties.getPublicUrls();
        permitAll(urls, registry);
    }

    /**
     * permit all inner url
     */
    public void permitAllInner(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        List<String> urls = properties.getInnerUrls();
        permitAll(urls, registry);
    }

    /**
     * 内部资源 RequestMatcher
     */
    public RequestMatcher innerRequestMatcher() {
        List<AntPathRequestMatcher> matchers = getMatchers(properties.getInnerUrls());
        return request -> matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    /**
     * public资源 RequestMatcher
     */
    public RequestMatcher publicRequestMatcher() {
        List<AntPathRequestMatcher> matchers = getMatchers(properties.getPublicUrls());
        return request -> matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    /**
     * 获取所有 permitAll 的 RequestMatcher，包含 public url 和 inner url
     */
    public RequestMatcher permitAllRequestMatcher() {
        List<String> all = new ArrayList<>();
        all.addAll(properties.getInnerUrls());
        all.addAll(properties.getPublicUrls());
        List<AntPathRequestMatcher> matchers = getMatchers(all);
        return request -> matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        properties.getPublicUrls().addAll(Arrays.asList(DEFAULT_IGNORE_URLS));

        /*
        - requestMappingHandlerMapping: defined by method 'requestMappingHandlerMapping' in class path resource
         [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]
        - controllerEndpointHandlerMapping: defined by method 'controllerEndpointHandlerMapping' in class path resource
         [org/springframework/boot/actuate/autoconfigure/endpoint/web/servlet/WebMvcEndpointManagementContextConfiguration.class]
         */
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping",
                RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        for (RequestMappingInfo info : map.keySet()) {
            HandlerMethod handlerMethod = map.get(info);

            // 获取类上的 @Permit
            Permit controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Permit.class);
            if (controller != null) {
                Class<?> beanType = handlerMethod.getBeanType();
                Method[] methods = beanType.getDeclaredMethods();
                Method method = handlerMethod.getMethod();
                if (ArrayUtil.contains(methods, method)) {
                    Optional.ofNullable(info.getPathPatternsCondition())
                            .ifPresent(con -> con.getPatterns()
                                    .forEach(url -> this.filterPath(url.getPatternString(), info, controller)));
                }
                continue;
            }

            // 获取方法中的 @Permit
            Permit method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Permit.class);
            if (method != null) {
                Optional.ofNullable(info.getPathPatternsCondition())
                        .ifPresent(con -> con.getPatterns()
                                .forEach(url -> this.filterPath(url.getPatternString(), info, method)));
            }
        }

        String publicUrls = String.join("\n", properties.getPublicUrls());
        String innerUrls = String.join("\n", properties.getInnerUrls());

        log.info("""
                
                
                ===========================
                PermitResolver
                PUBLIC URL:
                {}
                
                INNER URL:
                {}
                ===========================
                
                """, publicUrls, innerUrls);
    }

    /**
     * 存储结构: URL,Method
     */
    private void filterPath(String url, RequestMappingInfo info, Permit permit) {
        List<String> methodList = info.getMethodsCondition().getMethods()
                .stream().map(RequestMethod::name).collect(Collectors.toList());
        String resultUrl = ReUtil.replaceAll(url, PATTERN, "*");
        PermitMode mode = permit.mode();
        String method = CollUtil.isEmpty(methodList) ?
                "*" : CollUtil.join(methodList, VERTICAL_LINE);
        switch (mode) {
            case PUBLIC -> properties.addPublic(String.format("%s%s%s",
                    resultUrl, StrUtil.COMMA, method));
            case INNER -> properties.addInner(String.format("%s%s%s",
                    resultUrl, StrUtil.COMMA, method));
        }
    }

    private void permitAll(List<String> urls,
                           AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        for (String url : urls) {
            List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);
            if (CollUtil.size(urlAndMethod) == 1){
                // 如果 urlAndMethod.size() == 1，则method默认为 *
                urlAndMethod.add("*");
            }

            if (urlAndMethod.size() != 2) {
                log.warn("[PermitResolver] {} 无法配置 permitAll, 路径非法", url);
                continue;
            }

            if (StrUtil.equals(urlAndMethod.get(1), "*")) {
                registry.requestMatchers(urlAndMethod.get(0))
                        .permitAll();
                continue;
            }

            List<String> methods = StrUtil.split(urlAndMethod.get(1), VERTICAL_LINE);
            for (String method : methods) {
                registry.requestMatchers(HttpMethod.valueOf(method.toUpperCase()), urlAndMethod.get(0))
                        .permitAll();
            }
        }
    }

    private List<AntPathRequestMatcher> getMatchers(List<String> urls) {
        return urls.stream()
                .filter(url -> {
                    List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    return !urlAndMethod.isEmpty();
                })
                .flatMap(url -> {
                    List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    // 长度为1，默认method为*
                    if (urlAndMethod.size() == 1 || StrUtil.equals(urlAndMethod.get(1), "*")) {
                        AntPathRequestMatcher[] antPathRequestMatchers =
                                {new AntPathRequestMatcher(urlAndMethod.get(0))};
                        return Arrays.stream(antPathRequestMatchers);
                    }
                    List<String> methods = StrUtil.split(urlAndMethod.get(1), VERTICAL_LINE);
                    return Arrays.stream(methods.stream()
                            .map(method -> new AntPathRequestMatcher(urlAndMethod.get(0), method))
                            .toList().toArray(new AntPathRequestMatcher[methods.size()]));
                }).collect(Collectors.toList());
    }
}
