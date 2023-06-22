package com.ingot.framework.vc.module.reactive;

import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.common.VCVerify;
import com.ingot.framework.vc.common.VCVerifyUtils;
import com.ingot.framework.vc.properties.IngotVCProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>Description  : VCVerifyResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 3:44 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VCVerifyResolver implements InitializingBean {

    private final ReactiveWebApplicationContext applicationContext;
    private final IngotVCProperties properties;

    @Getter
    private final List<VCType> typeList = new ArrayList<>();
    @Getter
    private final List<ServerWebExchangeMatcher> requestMatcherList = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping",
                RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        for (RequestMappingInfo info : map.keySet()) {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法中的 @VCVerify
            VCVerify verify = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), VCVerify.class);
            if (verify != null) {
                Optional.of(info.getPatternsCondition())
                        .ifPresent(con -> con.getPatterns()
                                .forEach(url -> {
                                    List<String> methodList = info.getMethodsCondition().getMethods()
                                            .stream().map(RequestMethod::name).collect(Collectors.toList());
                                    String verifyUrl = VCVerifyUtils.getFinalVerifyPath(
                                            url.getPatternString(), methodList, verify);
                                    properties.getVerifyUrls().add(verifyUrl);
                                }));
            }
        }

        log.info("[VCVerifyResolver] verify urls = {}", properties.getVerifyUrls());

        // 遍历type
        VCType[] typeArray = VCType.values();
        for (VCType item : typeArray) {
            typeList.add(item);
            requestMatcherList.add(VCVerifyUtils.getServerWebExchangeMatcher(item, properties.getVerifyUrls()));
        }

        log.info("[VCVerifyResolver] afterPropertiesSet - typeList={}", typeList);
        log.info("[VCVerifyResolver] afterPropertiesSet - requestMatcherList={}", requestMatcherList);
    }
}
