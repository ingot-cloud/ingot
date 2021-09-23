package com.ingot.framework.security.oauth2.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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

    private final WebApplicationContext applicationContext;

    @Getter
    @Setter
    private List<String> publicUrls = new ArrayList<>();
    @Getter
    @Setter
    private List<String> innerUrls = new ArrayList<>();

    /**
     * permit all public url
     */
    public void permitAllPublic(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        List<String> urls = getPublicUrls();
        permitAll(urls, registry);
    }

    /**
     * permit all inner url
     */
    public void permitAllInner(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        List<String> urls = getInnerUrls();
        permitAll(urls, registry);
    }

    /**
     * 内部资源 RequestMatcher
     */
    public RequestMatcher innerRequestMatcher() {
        List<String> urls = getInnerUrls();
        List<AntPathRequestMatcher> matchers = urls.stream()
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
        return request -> matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
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

        log.info("[PermitResolver] public urls = {}", publicUrls);
        log.info("[PermitResolver] inner urls = {}", innerUrls);
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
                publicUrls.add(String.format("%s%s%s",
                        resultUrl, StrUtil.COMMA, method));
                break;
            case INNER:
                innerUrls.add(String.format("%s%s%s",
                        resultUrl, StrUtil.COMMA, method));
                break;
        }
    }

    private void permitAll(List<String> urls,
                           ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        for (String url : urls) {
            List<String> urlAndMethod = StrUtil.split(url, StrUtil.COMMA);

            if (urlAndMethod.size() != 2) {
                log.warn("--- {} 无法配置 permitAll, 路径非法", url);
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
}
