package com.ingot.framework.security.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
