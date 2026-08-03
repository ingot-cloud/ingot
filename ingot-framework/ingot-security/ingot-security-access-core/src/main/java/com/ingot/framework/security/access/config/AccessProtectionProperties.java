package com.ingot.framework.security.access.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 访问防护配置属性。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.access")
public class AccessProtectionProperties {

    private String mode = "local";

    private PolicyConfig policy = new PolicyConfig();

    private LoginFailureConfig loginFailure = new LoginFailureConfig();

    @Data
    public static class PolicyConfig {
        private Fallback fallback = new Fallback();
    }

    @Data
    public static class Fallback {
        private boolean localFloorEnabled = true;
    }

    @Data
    public static class LoginFailureConfig {
        private DimensionPolicy ip = defaultIp();
        private DimensionPolicy device = defaultDevice();
        private DimensionPolicy client = defaultClient();
        private DimensionPolicy accountIp = defaultAccountIp();
    }

    @Data
    public static class DimensionPolicy {
        private boolean enabled = true;
        private int maxAttempts = 50;
        private int windowMinutes = 1;
        private int blockTtlSec = 3600;
        private String blockKeyType = "IP";
    }

    private static DimensionPolicy defaultIp() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(50);
        p.setWindowMinutes(1);
        p.setBlockTtlSec(3600);
        p.setBlockKeyType("IP");
        return p;
    }

    private static DimensionPolicy defaultDevice() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(30);
        p.setWindowMinutes(5);
        p.setBlockTtlSec(1800);
        p.setBlockKeyType("DV");
        return p;
    }

    private static DimensionPolicy defaultClient() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(100);
        p.setWindowMinutes(5);
        p.setBlockTtlSec(3600);
        p.setBlockKeyType("CL");
        return p;
    }

    private static DimensionPolicy defaultAccountIp() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(10);
        p.setWindowMinutes(5);
        p.setBlockTtlSec(3600);
        p.setBlockKeyType("IP");
        return p;
    }
}
