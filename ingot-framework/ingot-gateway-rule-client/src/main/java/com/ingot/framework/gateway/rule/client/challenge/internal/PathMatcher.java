package com.ingot.framework.gateway.rule.client.challenge.internal;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 路径模式编译与匹配工具。
 *
 * <p>将 {@link EndpointPattern} 编译为 Spring {@link PathPattern}，支持 Ant 风格
 *（{@code **}、{@code *} 等）；HTTP 方法支持 {@code ANY} 通配。</p>
 *
 * <p>被 {@link CompiledChallengePolicy} 与网关 Sentinel 路径编译共用同一套 Ant 语义。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@UtilityClass
public class PathMatcher {

    private static final PathPatternParser PARSER = new PathPatternParser();

    /**
     * 编译原始路径模式列表；非法 path 静默跳过。
     */
    public static List<CompiledPattern> compile(List<EndpointPattern> raw) {
        List<CompiledPattern> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (EndpointPattern p : raw) {
            if (p == null || p.getPath() == null) {
                continue;
            }
            try {
                PathPattern pattern = PARSER.parse(p.getPath());
                result.add(new CompiledPattern(pattern, p.getMethod()));
            } catch (Exception ignore) {
                // skip invalid
            }
        }
        return result;
    }

    /**
     * 判断请求路径 + 方法是否命中任一已编译模式。
     *
     * @return 任一模式匹配返回 true
     */
    public static boolean matches(List<CompiledPattern> patterns, String path, HttpMethod method) {
        if (patterns == null || patterns.isEmpty() || path == null) {
            return false;
        }
        org.springframework.http.server.PathContainer container =
                org.springframework.http.server.PathContainer.parsePath(path);
        for (CompiledPattern p : patterns) {
            if (!p.methodMatches(method)) {
                continue;
            }
            if (p.pattern().matches(container)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 已编译的路径模式 + HTTP 方法约束。
     *
     * @param pattern Spring PathPattern
     * @param method  允许的方法名，或 {@link EndpointPattern#ANY_METHOD}
     */
    public record CompiledPattern(PathPattern pattern, String method) {
        /** 判断请求方法是否满足约束；ANY 时恒 true。 */
        public boolean methodMatches(HttpMethod request) {
            if (method == null || EndpointPattern.ANY_METHOD.equalsIgnoreCase(method)) {
                return true;
            }
            return request != null && method.equalsIgnoreCase(request.name());
        }
    }
}
