package com.ingot.cloud.gateway.config;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * <p>网关限流 KeyResolver 配置。</p>
 *
 * <p>所有 KeyResolver 优先读取由 {@link com.ingot.cloud.gateway.filter.RequestGlobalFilter}
 * 标准化后的 {@code X-Client-Real-IP} Header，避免反向代理 / K8s Service 后端
 * {@code getRemoteAddress()} 返回代理 IP 导致按代理 IP 统一限流。</p>
 *
 * <p><b>多 KeyResolver 选择规则</b>：Spring Cloud Gateway 的 {@code RequestRateLimiter}
 * 默认按类型注入唯一 {@link KeyResolver}，本类提供两个时必须有一个标 {@link Primary}
 * 作为默认；路由层若需要按设备指纹限流，可在 yml 中通过
 * {@code key-resolver: "#{@deviceKeyResolver}"} SpEL 显式指定。</p>
 *
 * <p>注：Sentinel Gateway 的限流走 {@code SentinelGatewayFilter} 与
 * {@link com.ingot.cloud.gateway.security.SentinelBlockHandler}，不依赖此处 KeyResolver；
 * 这两个 Bean 仅在 yml 显式启用 {@code RequestRateLimiter} filter 时使用。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Configuration
public class RateLimiterConfiguration {

    private static final String UNKNOWN = "unknown";

    /**
     * 按客户端真实 IP 限流。设为 {@link Primary}，作为 SCG 默认 KeyResolver。
     */
    @Bean(value = "remoteAddrKeyResolver")
    @Primary
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders()
                    .getFirst(HeaderConstants.CLIENT_REAL_IP);
            if (StrUtil.isBlank(ip) && exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(StrUtil.blankToDefault(ip, UNKNOWN));
        };
    }

    /**
     * 按 BFF 设备指纹限流，未携带时回落到客户端 IP。
     */
    @Bean(value = "deviceKeyResolver")
    public KeyResolver deviceKeyResolver() {
        return exchange -> {
            String device = exchange.getRequest().getHeaders()
                    .getFirst(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
            if (StrUtil.isNotBlank(device)) {
                return Mono.just(device);
            }
            String ip = exchange.getRequest().getHeaders()
                    .getFirst(HeaderConstants.CLIENT_REAL_IP);
            if (StrUtil.isBlank(ip) && exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(StrUtil.blankToDefault(ip, UNKNOWN));
        };
    }
}
