package com.ingot.framework.tss.xxljob.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : XXL-Job 任务调度配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.tss.xxljob")
public class XxlJobTaskProperties {
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    /**
     * Admin 配置
     */
    private Admin admin = new Admin();
    
    /**
     * 执行器配置
     */
    private Executor executor = new Executor();
    
    @Data
    public static class Admin {
        /**
         * Admin 地址列表（多个用逗号分隔）
         */
        private String addresses;
        
        /**
         * 访问令牌
         */
        private String accessToken;
    }
    
    @Data
    public static class Executor {
        /**
         * 执行器 AppName
         */
        private String appName;
        
        /**
         * 执行器注册地址
         */
        private String address;
        
        /**
         * 执行器IP
         */
        private String ip;
        
        /**
         * 执行器端口，小于等于0，端口自动生成
         */
        private int port = 0;
        
        /**
         * 执行器日志路径
         */
        private String logPath = "/data/applogs";
        
        /**
         * 执行器日志保留天数
         */
        private int logRetentionDays = 30;
    }
}
