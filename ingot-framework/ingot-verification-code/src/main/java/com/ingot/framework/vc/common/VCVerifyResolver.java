package com.ingot.framework.vc.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.vc.properties.IngotVCProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");
    private static final String VERTICAL_LINE = "|";

    private final WebApplicationContext applicationContext;
    private final IngotVCProperties properties;

    @Override
    public void afterPropertiesSet() throws Exception {
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping",
                RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        for (RequestMappingInfo info : map.keySet()) {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法中的 @Permit
            VCVerify verify = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), VCVerify.class);
            if (verify != null) {
                Optional.ofNullable(info.getPathPatternsCondition())
                        .ifPresent(con -> con.getPatterns()
                                .forEach(url -> this.filterPath(url.getPatternString(), info, verify)));
            }
        }

        log.info("[VCVerifyResolver] verify urls = {}", properties.getVerifyUrls());
    }

    /**
     * 获取指定验证码的 request matchers
     *
     * @param type {@link VCType}
     * @return {@link AntPathRequestMatcher}
     */
    public List<AntPathRequestMatcher> getMatchers(VCType type) {
        return getMatchers(type, properties.getVerifyUrls());
    }

    private List<AntPathRequestMatcher> getMatchers(VCType type, List<String> urls) {
        return urls.stream()
                .filter(url -> {
                    List<String> typeAndUrlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    return typeAndUrlAndMethod.size() == 3 && type == VCType.getEnum(typeAndUrlAndMethod.get(0));
                }).flatMap(url -> {
                    List<String> typeAndUrlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    String requestUrl = typeAndUrlAndMethod.get(1);
                    String requestMethod = typeAndUrlAndMethod.get(2);
                    // method
                    if (StrUtil.equals(requestMethod, "*")) {
                        AntPathRequestMatcher[] antPathRequestMatchers =
                                {new AntPathRequestMatcher(requestUrl)};
                        return Arrays.stream(antPathRequestMatchers);
                    }
                    List<String> methods = StrUtil.split(requestMethod, VERTICAL_LINE);
                    return Arrays.stream(methods.stream()
                            .map(method -> new AntPathRequestMatcher(requestUrl, method))
                            .collect(Collectors.toList()).toArray(new AntPathRequestMatcher[methods.size()]));
                }).collect(Collectors.toList());
    }

    private void filterPath(String url, RequestMappingInfo info, VCVerify verify) {
        List<String> methodList = info.getMethodsCondition().getMethods()
                .stream().map(RequestMethod::name).collect(Collectors.toList());
        String resultUrl = ReUtil.replaceAll(url, PATTERN, "*");
        VCType type = verify.type();
        String method = CollUtil.isEmpty(methodList) ?
                "*" : CollUtil.join(methodList, VERTICAL_LINE);
        String verifyUrl = String.format("%s%s%s%s%s",
                type.getValue(), StrUtil.COMMA, resultUrl, StrUtil.COMMA, method);
        properties.getVerifyUrls().add(verifyUrl);
    }

}
