package com.ingot.framework.sentinel.feign;

import java.lang.reflect.Proxy;
import java.util.Map;

import feign.RequestLine;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证动态白名单客户端及声明式客户端都保留 Sentinel，远程故障不转为成功。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class SentinelFeignBuilderTest {

    private static final String TARGET = "http://acceptance.invalid";

    @Test
    void dynamicInterfaceWithoutAnnotationBuildsAndInvokesThroughSentinel() {
        var client = successfulBuilder().target(DynamicRpc.class, TARGET);
        assertTrue(Proxy.getInvocationHandler(client) instanceof InSentinelInvocationHandler);
        assertEquals("ok", client.read());
    }

    @Test
    void annotatedInterfaceKeepsDefaultSentinelHandler() {
        var client = successfulBuilder().target(DeclaredRpc.class, TARGET);
        assertTrue(Proxy.getInvocationHandler(client) instanceof InSentinelInvocationHandler);
        assertEquals("ok", client.read());
    }

    @Test
    void dynamicRemoteFailureRemainsFailure() {
        var builder = new SentinelFeignBuilder();
        builder.client((request, options) -> {
            throw new IllegalStateException("remote unavailable");
        });
        var client = builder.target(DynamicRpc.class, TARGET);
        assertThrows(IllegalStateException.class, client::read);
    }

    private SentinelFeignBuilder successfulBuilder() {
        var builder = new SentinelFeignBuilder();
        builder.client((request, options) -> Response.builder()
            .request(request).status(200).headers(Map.of())
            .body("ok", java.nio.charset.StandardCharsets.UTF_8).build());
        return builder;
    }

    /** 动态客户端由可信服务名创建，不要求声明式注解。 */
    public interface DynamicRpc {
        /** @return 远程响应 */
        @RequestLine("GET /resource")
        String read();
    }

    /** 有注解客户端保留原构建分支。 */
    @FeignClient(name = "acceptance-declared")
    public interface DeclaredRpc {
        /** @return 远程响应 */
        @RequestLine("GET /resource")
        String read();
    }
}
