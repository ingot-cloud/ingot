package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.common.VCVerify;
import com.ingot.framework.vc.common.VCVerifyUtils;
import com.ingot.framework.vc.properties.IngotVCProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>Description  : VCVerifyResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/12.</p>
 * <p>Time         : 3:06 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VCVerifyResolver implements InitializingBean {
    private final WebApplicationContext applicationContext;
    private final IngotVCProperties properties;

    private final List<VCType> typeList = new ArrayList<>();
    private final List<RequestMatcher> requestMatcherList = new ArrayList<>();

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
                Optional.ofNullable(info.getPathPatternsCondition())
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
            requestMatcherList.add(VCVerifyUtils.getMatcher(item, properties.getVerifyUrls()));
        }

        log.info("[VCVerifyResolver] afterPropertiesSet - typeList={}", typeList);
        log.info("[VCVerifyResolver] afterPropertiesSet - requestMatcherList={}", requestMatcherList);
    }

    /**
     * 匹配请求
     *
     * @param request  {@link HttpServletRequest}
     * @param consumer {@link Consumer}
     */
    public void matches(HttpServletRequest request, Consumer<VCType> consumer) {
        int len = typeList.size();
        for (int i = 0; i < len; i++) {
            RequestMatcher matcher = requestMatcherList.get(i);
            if (matcher.matches(request)) {
                VCType type = typeList.get(i);
                consumer.accept(type);
                break;
            }
        }
    }

}
