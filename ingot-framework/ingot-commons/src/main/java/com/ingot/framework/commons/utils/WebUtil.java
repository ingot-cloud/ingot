package com.ingot.framework.commons.utils;

import java.util.Objects;

import cn.hutool.core.util.ArrayUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>Description  : WebUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/4.</p>
 * <p>Time         : 上午10:25.</p>
 */
@Slf4j
public final class WebUtil extends org.springframework.web.util.WebUtils {

    /**
     * Gets request.
     *
     * @return the request
     */
    public static HttpServletRequest getRequest() {
        try {
            return Objects.requireNonNull(
                            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()))
                    .getRequest();
        } catch (Exception e) {
            log.debug("[WebUtils] - RequestUtils.getRequest() error, error={}",
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取请求来源 IP。
     *
     * <p>优先读取网关标准化头 {@link HeaderConstants#INNER_CLIENT_REAL_IP}，再按代理头顺序回退，
     * 最后使用 {@link HttpServletRequest#getRemoteAddr()}。</p>
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    public static String getClientIP(HttpServletRequest request) {
        return getClientIPByHeader(request, HeaderConstants.REQUEST_SOURCE_IP_HEADERS);
    }

    /**
     * 获取请求来源 IP，在默认 Header 列表之后追加自定义 Header。
     *
     * <p>需要注意的是，使用此方法获取的客户端 IP 须在 Http 服务器（例如 Nginx）中正确配置头信息，
     * 否则容易造成 IP 伪造。</p>
     *
     * @param request      请求对象
     * @param extraHeaders 额外自定义 Header，按追加顺序参与解析
     * @return 客户端 IP
     */
    public static String getClientIP(HttpServletRequest request, String... extraHeaders) {
        String[] headers = HeaderConstants.REQUEST_SOURCE_IP_HEADERS;
        if (ArrayUtil.isNotEmpty(extraHeaders)) {
            headers = ArrayUtil.addAll(headers, extraHeaders);
        }
        return getClientIPByHeader(request, headers);
    }

    /**
     * 按指定 Header 顺序获取请求来源 IP。
     *
     * @param request     请求对象
     * @param headerNames 自定义 Header 解析顺序
     * @return 客户端 IP
     */
    public static String getClientIPByHeader(HttpServletRequest request, String... headerNames) {
        return ClientIpResolver.resolve(headerNames, request::getHeader, request::getRemoteAddr);
    }

}
