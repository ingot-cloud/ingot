package com.ingot.framework.commons.utils;

import java.util.function.Function;
import java.util.function.Supplier;

import cn.hutool.core.net.NetUtil;

/**
 * <p>客户端 IP 解析器，统一 Servlet 与 Reactive 的请求来源 IP 获取逻辑。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * 按 Header 优先级解析客户端 IP，均无效时回退 remoteAddress。
     *
     * @param headers            按优先级排列的 Header 名称
     * @param headerGetter       按名称读取 Header 值
     * @param remoteAddrSupplier 读取 socket 远端地址
     * @return 客户端 IP，无法解析时返回空字符串
     */
    public static String resolve(String[] headers,
                                 Function<String, String> headerGetter,
                                 Supplier<String> remoteAddrSupplier) {
        for (String header : headers) {
            String ip = headerGetter.apply(header);
            if (!NetUtil.isUnknown(ip)) {
                return IpUtil.normalize(NetUtil.getMultistageReverseProxyIp(ip));
            }
        }
        String remote = remoteAddrSupplier.get();
        return NetUtil.isUnknown(remote) ? ""
                : IpUtil.normalize(NetUtil.getMultistageReverseProxyIp(remote));
    }
}
