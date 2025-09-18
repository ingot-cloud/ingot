package com.ingot.framework.vc.common;

import java.util.List;

import cn.hutool.core.text.AntPathMatcher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;

/**
 * <p>Description  : RequestMeta.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 9:44 AM.</p>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMeta {
    private final String pattern;
    private final List<String> methods;
    private final AntPathMatcher antPathMatcher;
    private final boolean permitAllMethod;

    public static RequestMeta create(String pattern, List<String> methods) {
        AntPathMatcher matcher = new AntPathMatcher();
        return new RequestMeta(pattern, methods, matcher, methods.contains("*"));
    }

    /**
     * 匹配校验
     *
     * @param path   路径
     * @param method {@link HttpMethod}
     * @return 是否匹配
     */
    public boolean matches(String path, HttpMethod method) {
        if (!permitAllMethod
                && methods.stream()
                .map(HttpMethod::valueOf)
                .noneMatch(item -> item == method)) {
            return false;
        }
        return antPathMatcher.match(this.pattern, path);
    }
}
