package com.ingot.framework.vc.common;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : VCVerifyUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 3:46 PM.</p>
 */
public class VCVerifyUtils {
    public static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");
    public static final String VERTICAL_LINE = "|";

    /**
     * 获取指定验证码的 {@link RequestMatcher}
     *
     * @param type {@link VCType}
     * @param urls url list
     * @return {@link RequestMatcher}
     */
    public static RequestMatcher getMatcher(VCType type, List<String> urls) {
        List<PathPatternRequestMatcher> matchers = getMatchers(type, urls);
        return request -> matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    /**
     * 获取 {@link ServerWebExchangeMatcher}
     *
     * @param type {@link VCType}
     * @param urls url list
     * @return {@link ServerWebExchangeMatcher}
     */
    public static ServerWebExchangeMatcher getServerWebExchangeMatcher(VCType type, List<String> urls) {
        List<RequestMeta> matchers = getRequestMeta(type, urls);
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();
            return Mono.just(matchers.stream().anyMatch(matcher -> matcher.matches(path, method)));
        };
    }

    /**
     * 获取包装后的验证路径
     *
     * @param url        url
     * @param methodList method list
     * @param verify     {@link VCVerify}
     * @return verify url
     */
    public static String getFinalVerifyPath(String url, List<String> methodList, VCVerify verify) {
        String resultUrl = ReUtil.replaceAll(url, VCVerifyUtils.PATTERN, "*");
        VCType type = verify.type();
        String method = CollUtil.isEmpty(methodList) ?
                "*" : CollUtil.join(methodList, VCVerifyUtils.VERTICAL_LINE);
        return String.format("%s%s%s%s%s",
                type.getValue(), StrUtil.COMMA, resultUrl, StrUtil.COMMA, method);
    }

    private static List<PathPatternRequestMatcher> getMatchers(VCType type, List<String> urls) {
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
                        PathPatternRequestMatcher matcher = PathPatternRequestMatcher
                                .withDefaults()
                                .matcher(requestUrl);
                        PathPatternRequestMatcher[] pathPatternRequestMatchers = {matcher};
                        return Arrays.stream(pathPatternRequestMatchers);
                    }
                    List<String> methods = StrUtil.split(requestMethod, VERTICAL_LINE);
                    return Arrays.stream(methods.stream()
                            .map(method -> PathPatternRequestMatcher
                                    .withDefaults()
                                    .matcher(HttpMethod.valueOf(method), requestUrl))
                            .toList().toArray(new PathPatternRequestMatcher[methods.size()]));
                }).collect(Collectors.toList());
    }

    private static List<RequestMeta> getRequestMeta(VCType type, List<String> urls) {
        return urls.stream()
                .filter(url -> {
                    List<String> typeAndUrlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    return typeAndUrlAndMethod.size() == 3 && type == VCType.getEnum(typeAndUrlAndMethod.get(0));
                })
                .map(url -> {
                    List<String> typeAndUrlAndMethod = StrUtil.split(url, StrUtil.COMMA);
                    String requestUrl = typeAndUrlAndMethod.get(1);
                    String requestMethod = typeAndUrlAndMethod.get(2);
                    List<String> methods = StrUtil.split(requestMethod, VERTICAL_LINE, true, true);
                    return RequestMeta.create(requestUrl, methods);
                }).collect(Collectors.toList());
    }
}
