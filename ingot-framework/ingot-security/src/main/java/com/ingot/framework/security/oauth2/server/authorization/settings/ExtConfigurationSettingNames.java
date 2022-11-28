package com.ingot.framework.security.oauth2.server.authorization.settings;

import com.ingot.framework.security.common.constants.TokenAuthType;

/**
 * <p>Description  : 扩展 ConfigurationSettingNames.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 3:04 PM.</p>
 */
public final class ExtConfigurationSettingNames {
    private static final String SETTINGS_NAMESPACE = "ingot.settings.";
    private static final String CLIENT_SETTINGS_NAMESPACE = SETTINGS_NAMESPACE.concat("client.");
    private static final String TOKEN_SETTINGS_NAMESPACE = SETTINGS_NAMESPACE.concat("token.");

    /**
     * 客户端状态
     */
    public static final String CLIENT_STATUS = CLIENT_SETTINGS_NAMESPACE.concat("status");
    /**
     * Token认证类型 {@link TokenAuthType}
     */
    public static final String TOKEN_AUTH_TYPE = TOKEN_SETTINGS_NAMESPACE.concat("auth-type");
}
