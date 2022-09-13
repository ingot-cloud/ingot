package com.ingot.framework.security.oauth2.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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

    private final WebApplicationContext applicationContext;
    private final IngotOAuth2ResourceProperties properties;

    /**
     * permit all public url
     */
    public void permitAllPublic(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        List<String> urls = properties.getPublicUrls();
        permitAll(urls, registry);
    }

    /**
     * permit all inner url
     */
    public void permitAllInner(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
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
                    Optional.ofNullable(info.getPatternsCondition())
                            .ifPresent(con -> con.getPatterns()
                                    .forEach(url -> this.filterPath(url, info, controller)));
                }
                continue;
            }

            // 获取方法中的 @Permit
            Permit method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Permit.class);
            Optional.ofNullable(method).flatMap(an -> Optional.ofNullable(info.getPatternsCondition()))
                    .ifPresent(con ->
                            con.getPatterns()
                                    .forEach(url -> this.filterPath(url, info, method)));
        }

        log.info("[PermitResolver] public urls = {}", properties.getPublicUrls());
        log.info("[PermitResolver] inner urls = {}", properties.getInnerUrls());
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
            case PUBLIC:
                properties.addPublic(String.format("%s%s%s",
                        resultUrl, StrUtil.COMMA, method));
                break;
            case INNER:
                properties.addInner(String.format("%s%s%s",
                        resultUrl, StrUtil.COMMA, method));
                break;
        }
    }

    private void permitAll(List<String> urls,
                           ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        for (String url : urls) {
            List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);

            if (urlAndMethod.size() != 2) {
                log.warn("[PermitResolver] {} 无法配置 permitAll, 路径非法", url);
                continue;
            }

            if (StrUtil.equals(urlAndMethod.get(1), "*")) {
                registry.antMatchers(urlAndMethod.get(0)).permitAll();
                continue;
            }

            List<String> methods = StrUtil.split(urlAndMethod.get(1), VERTICAL_LINE);
            for (String method : methods) {
                registry.antMatchers(HttpMethod.valueOf(method), urlAndMethod.get(0)).permitAll();
            }
        }
    }

    private List<AntPathRequestMatcher> getMatchers(List<String> urls) {
        return urls.stream()
                .filter(url -> {
                    List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    return urlAndMethod.size() == 2;
                })
                .flatMap(url -> {
                    List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    if (StrUtil.equals(urlAndMethod.get(1), "*")) {
                        AntPathRequestMatcher[] antPathRequestMatchers =
                                {new AntPathRequestMatcher(urlAndMethod.get(0))};
                        return Arrays.stream(antPathRequestMatchers);
                    }
                    List<String> methods = StrUtil.split(urlAndMethod.get(1), VERTICAL_LINE);
                    return Arrays.stream(methods.stream()
                            .map(method -> new AntPathRequestMatcher(urlAndMethod.get(0), method))
                            .collect(Collectors.toList()).toArray(new AntPathRequestMatcher[methods.size()]));
                }).collect(Collectors.toList());
    }
}
