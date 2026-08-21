package com.ingot.framework.security.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * <p>Description  : 安全配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/2/16.</p>
 * <p>Time         : 10:34 AM.</p>
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "ingot.security")
public class InSecurityProperties {
    /**
     * 忽略租户验证的角色编码列表
     */
    private List<String> ignoreTenantValidateRoleCodeList = new ArrayList<>();
    
    /**
     * JWK 管理配置
     */
    private Jwk jwk = new Jwk();

    /**
     * 在线会话配置
     */
    private Session session = new Session();

    @Setter
    @Getter
    public static class Session {
        /**
         * 会话存储不可用时的宽限期（默认 30 秒）
         * <p>仅覆盖 Redis 读写异常：宽限期内签名仍有效的 JWT 可降级通过，超期后一律拒绝。
         * 「会话键不存在」不属于宽限范围，一律按已下线拒绝。</p>
         */
        private Duration storeUnavailableGrace = Duration.ofSeconds(30);

        /**
         * 同一 IP 会话集合 {@code session:ip:{tenantId}:{ip}} 的最大成员数
         * <p>超出后新登录不再写入该索引，管理面按 IP 查询在超限出口 IP 上可能不完整。
         * {@code 0} 表示不限制。</p>
         */
        private int ipSetMaxMembers = 1000;

        /**
         * 并发会话策略来源：{@link PolicySourceMode#LOCAL} 读本地地板配置，
         * {@link PolicySourceMode#REMOTE} 走安全中心。
         * <p>与 {@code ingot.security.access.mode} 同义：未部署安全中心时保持 local，
         * 生产部署安全中心后切 remote 并由 LKG / 地板兜底。</p>
         */
        private PolicySourceMode mode = PolicySourceMode.LOCAL;

        /**
         * 并发策略远端拉取的缓存与降级参数
         */
        @NestedConfigurationProperty
        private Policy policy = new Policy();

        /**
         * 并发会话约束；{@code mode=local} 时直接生效，{@code mode=remote} 时作为 Nacos 地板
         */
        @NestedConfigurationProperty
        private Concurrency concurrency = new Concurrency();
    }

    @Setter
    @Getter
    public static class Policy {
        /**
         * 是否启用 {@code remote → LKG → 地板} 降级阶梯；关闭后远端不可用直接拒绝新登录
         */
        private boolean resilienceEnabled = true;

        /**
         * 降级末级配置
         */
        @NestedConfigurationProperty
        private Fallback fallback = new Fallback();

        /**
         * 策略快照的分层缓存参数
         */
        @NestedConfigurationProperty
        private Cache cache = new Cache();
    }

    @Setter
    @Getter
    public static class Fallback {
        /**
         * 远端不可用且无 LKG 时是否回落本地地板
         * <p>关闭后该场景 fail-closed 拒绝新登录，而不是静默放开为无限并发。</p>
         */
        private boolean localFloorEnabled = true;
    }

    @Setter
    @Getter
    public static class Cache {
        /**
         * 是否启用 L1 进程内缓存
         */
        private boolean l1Enabled = true;

        /**
         * L1 存活时间，同时是失效广播丢失时的最长 stale 窗口
         */
        private Duration l1Ttl = Duration.ofMinutes(1);

        /**
         * L1 最大条目数；策略为单 key 快照，取小值即可
         */
        private long l1MaximumSize = 8;

        /**
         * 是否启用 L2 Redis 共享缓存
         */
        private boolean l2Enabled = true;

        /**
         * L2 存活时间
         */
        private Duration l2Ttl = Duration.ofMinutes(10);

        /**
         * L2 热缓存 Redis key
         */
        private String l2RedisKey = RedisKeyConstants.SessionPolicy.CONCURRENCY_SNAPSHOT;

        /**
         * LKG 快照 Redis key；长存不过期，与 L2 命名空间区分
         */
        private String lkgRedisKey = RedisKeyConstants.SessionPolicy.CONCURRENCY_LKG;
    }

    @Setter
    @Getter
    public static class Concurrency {
        /**
         * 并发会话约束总开关
         * <p>关闭后登录路径退回「只认 Client 的 UNIQUE / STANDARD」，作为策略执行的紧急回退。</p>
         */
        private boolean enabled = true;

        /**
         * 允许的最大并发会话数；{@code 0} 表示无限
         * <p>无限时 Client 配置为 UNIQUE 仍按单会话处理（缺省 N=1），显式值优先于 Client 配置。</p>
         */
        private int maxSessions = 0;

        /**
         * 会话数统计维度
         */
        private SessionConcurrencyDimension dimension = SessionConcurrencyDimension.USER_CLIENT;

        /**
         * 超限处置方式
         */
        private SessionOverflowStrategy overflow = SessionOverflowStrategy.KICK_OLDEST;

        /**
         * 管理用户（{@code UserTypeEnum.ADMIN}）是否强制单会话
         */
        private boolean adminForbidConcurrent = false;
    }
    
    @Setter
    @Getter
    public static class Jwk {
        /**
         * 主密钥，用于加密 Redis 中的私钥
         * 建议从环境变量或配置中心获取，不要硬编码在配置文件中
         * 可以通过 ${AUTH_JWK_MASTER_KEY} 从环境变量读取
         */
        private String masterKey;
        
        /**
         * 是否启用私钥加密（默认启用）
         */
        private boolean enableEncryption = true;
        
        /**
         * 密钥生命周期（默认 90 天）
         */
        private Duration keyLifetime = Duration.ofDays(90);
        
        /**
         * 密钥轮换宽限期（默认 7 天）
         * 在此期间，旧密钥仍可用于验证 JWT
         */
        private Duration keyGracePeriod = Duration.ofHours(2);
        
        /**
         * 最大活跃密钥数量（默认 3 个）
         */
        private int maxActiveKeys = 3;
        
        /**
         * 资源服务器 JWK 缓存刷新间隔（默认 5 分钟）
         */
        private Duration cacheRefreshInterval = Duration.ofMinutes(5);
    }
}
