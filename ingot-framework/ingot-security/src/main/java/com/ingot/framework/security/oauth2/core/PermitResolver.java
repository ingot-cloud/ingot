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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final WebApplicationContext applicationContext;

    @Getter
    @Setter
    private List<String> publicUrls = new ArrayList<>();
    @Getter
    @Setter
    private List<String> innerUrls = new ArrayList<>();

    /**
     * permit all url
     */
    public void permitAll(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry) {
        for (String url : getPublicUrls()) {
            List<String> urlAndMethod = StrUtil.split(url, "|");

            // method 为空，则permit所有方法
            if (urlAndMethod.size() == 1) {
                registry.antMatchers(urlAndMethod.get(0)).permitAll();
                continue;
            }

            // url对应方法permitAll
            if (urlAndMethod.size() == 2) {
                for (String method : StrUtil.split(urlAndMethod.get(1), StrUtil.COMMA)) {
                    registry.antMatchers(HttpMethod.valueOf(method), urlAndMethod.get(0)).permitAll();
                }
                continue;
            }

            log.warn("--- {} 无法配置 permitAll", url);
        }
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
                            .ifPresent(con -> con.getPatterns().forEach(url -> filterPath(url, info, controller.mode())));
                }
                continue;
            }

            // 获取方法中的 @Permit
            Permit method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Permit.class);
            Optional.ofNullable(method).flatMap(an -> Optional.ofNullable(info.getPatternsCondition()))
                    .ifPresent(con ->
                            con.getPatterns()
                                    .forEach(url -> this.filterPath(url, info, method.mode())));
        }
    }

    private void filterPath(String url, RequestMappingInfo info, PermitMode mode) {
        List<String> methodList = info.getMethodsCondition().getMethods().stream().map(RequestMethod::name)
                .collect(Collectors.toList());
        String resultUrl = ReUtil.replaceAll(url, PATTERN, "*");
        if (CollUtil.isEmpty(methodList)) {
            switch (mode) {
                case PUBLIC:
                    publicUrls.add(resultUrl);
                    break;
                case INNER:
                    innerUrls.add(resultUrl);
                    break;
            }
        } else {
            switch (mode) {
                case PUBLIC:
                    publicUrls.add(String.format("%s|%s", resultUrl, CollUtil.join(methodList, StrUtil.COMMA)));
                    break;
                case INNER:
                    innerUrls.add(String.format("%s|%s", resultUrl, CollUtil.join(methodList, StrUtil.COMMA)));
                    break;
            }

        }
    }
}
