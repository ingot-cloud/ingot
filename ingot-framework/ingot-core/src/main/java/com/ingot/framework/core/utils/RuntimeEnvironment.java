package com.ingot.framework.core.utils;

import java.util.Arrays;

import org.springframework.core.env.Environment;

/**
 * <p>获取当前激活环境</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class RuntimeEnvironment {
    private static final String DEFAULT_DEV = "dev";
    private static final String DEFAULT_TEST = "test";
    private static final String DEFAULT_PROD = "prod";

    private final Environment environment;

    public RuntimeEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取所有激活的环境
     */
    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }

    /**
     * 判断Dev环境
     */
    public boolean isDev() {
        return isActive(DEFAULT_DEV);
    }

    /**
     * 判断Test环境
     */
    public boolean isTest() {
        return isActive(DEFAULT_TEST);
    }

    /**
     * 判断Prod环境
     */
    public boolean isProd() {
        return isActive(DEFAULT_PROD);
    }

    /**
     * 判断指定环境是否激活
     */
    public boolean isActive(String profile) {
        return Arrays.asList(environment.getActiveProfiles())
                .contains(profile);
    }

    /**
     * 获取主要运行环境
     */
    public String getPrimaryProfile() {
        String[] activeProfiles = environment.getActiveProfiles();

        if (activeProfiles.length == 0) {
            String[] defaultProfiles = environment.getDefaultProfiles();
            return defaultProfiles.length > 0 ? defaultProfiles[0] : "default";
        }

        return activeProfiles[0];
    }
}
