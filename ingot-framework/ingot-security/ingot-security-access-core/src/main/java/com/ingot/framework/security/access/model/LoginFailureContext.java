package com.ingot.framework.security.access.model;

/**
 * 登录失败/成功上下文。
 *
 * @author jy
 * @since 1.0.0
 */
public record LoginFailureContext(
        String ip,
        String deviceId,
        String clientId,
        String username,
        String userType
) {
}
