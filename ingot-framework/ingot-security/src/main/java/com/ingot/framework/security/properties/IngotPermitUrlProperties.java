package com.ingot.framework.security.properties;

import cn.hutool.core.util.ReUtil;
import com.ingot.framework.security.annotation.Permit;
import com.ingot.framework.security.model.enums.PermitModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * <p>Description  : IngotPermitUrlProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/13.</p>
 * <p>Time         : 4:24 PM.</p>
 */
@Slf4j
@Configuration
@AllArgsConstructor
@ConfigurationProperties(prefix = "ingot.oauth2.resource")
public class IngotPermitUrlProperties implements InitializingBean {
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");
    private final WebApplicationContext applicationContext;

    /**
     * 忽略鉴权的 url
     */
    @Getter
    @Setter
    private List<String> ignoreUrls = new ArrayList<>();

    /**
     * 忽略 {@link com.ingot.framework.security.core.filter.UserAuthenticationFilter} 的 url
     */
    @Getter
    @Setter
    private List<String> ignoreUserUrls = new ArrayList<>();

    @SneakyThrows
    @Override public void afterPropertiesSet() {
        // 添加全部permit的Url, 忽略鉴权的 url 一定要忽略用户认证
        ignoreUserUrls.addAll(ignoreUrls);

        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 *
            Permit method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Permit.class);
            Optional.ofNullable(method)
                    .ifPresent(item -> info.getPatternsCondition().getPatterns()
                            .forEach(url -> loadUrl(method.model(), url)));

            // 获取类上边的注解, 替代path variable 为 *
            Permit controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Permit.class);
            Optional.ofNullable(controller)
                    .ifPresent(item -> info.getPatternsCondition().getPatterns()
                            .forEach(url -> loadUrl(controller.model(), url)));
        });

        log.info(">>> IgnoreUserAuthenticationProperties, 忽略用户鉴权 URL={}", ignoreUserUrls);
        log.info(">>> IgnoreUserAuthenticationProperties, 忽略全部鉴权 URL={}", ignoreUrls);
    }

    private void loadUrl(PermitModel model, String url){
        switch (model){
            case PUBLIC:
            case INNER:
                ignoreUrls.add(ReUtil.replaceAll(url, PATTERN, "*"));
            case USER:
                ignoreUserUrls.add(ReUtil.replaceAll(url, PATTERN, "*"));
                break;
        }
    }
}
