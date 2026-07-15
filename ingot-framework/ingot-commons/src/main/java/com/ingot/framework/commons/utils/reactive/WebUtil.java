package com.ingot.framework.commons.utils.reactive;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.utils.ClientIpResolver;
import com.ingot.framework.commons.utils.IpUtil;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * <p>Description  : WebUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-19.</p>
 * <p>Time         : 09:27.</p>
 */
public final class WebUtil {

    private WebUtil() {
    }

    /**
     * 获取请求来源 IP。
     *
     * @param request ServerRequest
     * @return 客户端 IP
     */
    public static String getClientIP(ServerRequest request) {
        return ClientIpResolver.resolve(
                HeaderConstants.REQUEST_SOURCE_IP_HEADERS,
                header -> {
                    List<String> values = request.headers().header(header);
                    return CollUtil.isEmpty(values) ? null : values.getFirst();
                },
                () -> request.remoteAddress()
                        .map(IpUtil::fromInetSocketAddress)
                        .orElse(null));
    }

    /**
     * 获取请求来源 IP。
     *
     * @param request ServerHttpRequest
     * @return 客户端 IP
     */
    public static String getClientIP(ServerHttpRequest request) {
        return ClientIpResolver.resolve(
                HeaderConstants.REQUEST_SOURCE_IP_HEADERS,
                header -> request.getHeaders().getFirst(header),
                () -> IpUtil.fromInetSocketAddress(request.getRemoteAddress()));
    }
}
