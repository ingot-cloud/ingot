package com.ingot.framework.dict.client.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 字典客户端配置项。
 *
 * @author jy
 * @since 2026/4/25
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.dict.client")
public class DictClientProperties {

    /**
     * 客户端模式：
     * <ul>
     *     <li>{@link Mode#AUTO}：自动选择，本地优先（默认）</li>
     *     <li>{@link Mode#LOCAL}：强制使用本地实现</li>
     *     <li>{@link Mode#REMOTE}：强制使用 RPC 实现</li>
     *     <li>{@link Mode#NONE}：禁用，业务方需自行注入实现</li>
     * </ul>
     */
    private Mode mode = Mode.AUTO;

    /**
     * 是否启用 L1 客户端缓存（Caffeine 进程内缓存）
     */
    private boolean cacheEnabled = true;

    /**
     * L1 缓存最大条目数
     */
    private long cacheMaximumSize = 1024;

    /**
     * L1 缓存有效期
     */
    private Duration cacheTtl = Duration.ofMinutes(5);

    /**
     * 是否启用 L2 Redis 共享缓存
     */
    private boolean redisEnabled = true;

    /**
     * L2 Redis key 前缀（最终 key = {@code <redisKeyPrefix>:dict:items:...}）
     */
    private String redisKeyPrefix = "in";

    /**
     * L2 Redis 缓存有效期
     */
    private Duration redisTtl = Duration.ofMinutes(30);

    /**
     * 是否启用跨节点失效广播订阅（依赖 ingot-event-bus）
     */
    private boolean invalidationEnabled = true;

    public enum Mode {
        AUTO, LOCAL, REMOTE, NONE
    }
}
