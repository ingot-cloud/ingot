package com.ingot.framework.sentinel.webmvc.callback;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>自定义 {@link RequestOriginParser}。</p>
 *
 * <p>解析顺序：</p>
 * <ol>
 *     <li>{@code X-Client-Real-IP} —— 由网关 {@code RequestGlobalFilter} 标准化注入</li>
 *     <li>{@code X-Forwarded-For} —— 通用代理协议（取首段）</li>
 *     <li>{@code X-Real-IP} —— Nginx 常用</li>
 *     <li>{@link HttpServletRequest#getRemoteAddr()} —— 兜底</li>
 * </ol>
 *
 * <p>Header 名称与 {@code HeaderConstants.CLIENT_REAL_IP} 保持一致，未通过 {@code ingot-commons}
 * 引入是因为本模块对 {@code ingot-core} 仅是 compileOnly 依赖。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public class InRequestOriginParser implements RequestOriginParser {

    private static final String UNKNOWN = "unknown";

    /**
     * Header 名称数组，按解析优先级排列。
     */
    private static final String[] IP_HEADERS = {
            "X-Client-Real-IP",
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    @Override
    public String parseOrigin(HttpServletRequest request) {
        for (String name : IP_HEADERS) {
            String value = request.getHeader(name);
            if (isValid(value)) {
                int comma = value.indexOf(',');
                return comma > 0 ? value.substring(0, comma).trim() : value.trim();
            }
        }
        String remote = request.getRemoteAddr();
        return isValid(remote) ? remote : UNKNOWN;
    }

    private static boolean isValid(String value) {
        return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value);
    }
}
