package com.ingot.framework.vc.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        List<AntPathRequestMatcher> matchers = getMatchers(type, urls);
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
        List<PathPatternParserServerWebExchangeMatcher> matchers = getServerWebExchangeMatchers(type, urls);
        return exchange -> {
            boolean isMatch = matchers.stream().anyMatch(matcher -> {
                ServerWebExchangeMatcher.MatchResult result = matcher.matches(exchange).blockOptional().orElse(null);
                return result != null && result.isMatch();
            });
            return isMatch ? ServerWebExchangeMatcher.MatchResult.match()
                    : ServerWebExchangeMatcher.MatchResult.notMatch();
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

    private static List<AntPathRequestMatcher> getMatchers(VCType type, List<String> urls) {
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

    private static List<PathPatternParserServerWebExchangeMatcher> getServerWebExchangeMatchers(VCType type, List<String> urls) {
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
                        PathPatternParserServerWebExchangeMatcher[] matchers =
                                {new PathPatternParserServerWebExchangeMatcher(requestUrl)};
                        return Arrays.stream(matchers);
                    }
                    List<String> methods = StrUtil.split(requestMethod, VERTICAL_LINE);
                    return Arrays.stream(methods.stream()
                            .map(method -> new PathPatternParserServerWebExchangeMatcher(requestUrl, HttpMethod.resolve(method)))
                            .collect(Collectors.toList()).toArray(new PathPatternParserServerWebExchangeMatcher[methods.size()]));
                }).collect(Collectors.toList());
    }
}
