package com.ingot.cloud.auth.api;

import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <p>启用 Auth 服务对内 Feign 契约的自动装配。</p>
 *
 * <p>消费方只需引入 {@code ingot-auth-api} 依赖即可注入本模块 {@code rpc} 包下的
 * {@code Remote*Service}，无需自行声明 {@link EnableFeignClients} 扫描路径。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@EnableFeignClients
@ConditionalOnClass(Feign.class)
public class EnableAPIConfiguration {
}
