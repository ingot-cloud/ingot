package com.ingot.framework.gateway.rule.client.challenge.internal;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@link EndpointPattern} 编译为 Spring {@link PathPattern}，支持 Ant 风格匹配。
 *
 * @author jy
 * @since 2026/5/26
 */
public final class PathMatcher {

    private static final PathPatternParser PARSER = new PathPatternParser();

    private PathMatcher() {
    }

    public static List<CompiledPattern> compile(List<EndpointPattern> raw) {
        List<CompiledPattern> result = new ArrayList<>();
        if (raw == null) return result;
        for (EndpointPattern p : raw) {
            if (p == null || p.getPath() == null) continue;
            try {
                PathPattern pattern = PARSER.parse(p.getPath());
                result.add(new CompiledPattern(pattern, p.getMethod()));
            } catch (Exception ignore) {
                // skip invalid
            }
        }
        return result;
    }

    public static boolean matches(List<CompiledPattern> patterns, String path, HttpMethod method) {
        if (patterns == null || patterns.isEmpty() || path == null) return false;
        org.springframework.http.server.PathContainer container =
                org.springframework.http.server.PathContainer.parsePath(path);
        for (CompiledPattern p : patterns) {
            if (!p.methodMatches(method)) continue;
            if (p.pattern().matches(container)) return true;
        }
        return false;
    }

    public record CompiledPattern(PathPattern pattern, String method) {
        public boolean methodMatches(HttpMethod request) {
            if (method == null || EndpointPattern.ANY_METHOD.equalsIgnoreCase(method)) return true;
            return request != null && method.equalsIgnoreCase(request.name());
        }
    }
}
