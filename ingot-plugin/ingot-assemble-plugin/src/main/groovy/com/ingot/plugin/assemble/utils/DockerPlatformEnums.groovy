package com.ingot.plugin.assemble.utils

/**
 * <p>Description  : DockerPlatformEnums.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/3/11.</p>
 * <p>Time         : 09:36.</p>
 */
enum DockerPlatformEnums {
    LINUX_AMD64("linux/amd64"),
    LINUX_ARM64("linux/arm64"),

    private String value;

    DockerPlatformEnums(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}