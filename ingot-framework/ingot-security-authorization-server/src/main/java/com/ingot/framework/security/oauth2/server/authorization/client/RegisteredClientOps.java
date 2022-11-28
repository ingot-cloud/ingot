package com.ingot.framework.security.oauth2.server.authorization.client;

import java.util.Map;
import java.util.Optional;

import com.ingot.framework.security.common.constants.TokenAuthType;
import com.ingot.framework.security.oauth2.server.authorization.settings.ExtConfigurationSettingNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * <p>Description  : RegisteredClientOps.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 7:22 PM.</p>
 */
public final class RegisteredClientOps {
    private final RegisteredClient client;

    private RegisteredClientOps(RegisteredClient client) {
        this.client = client;
    }

    public static RegisteredClientOps of(RegisteredClient client) {
        return new RegisteredClientOps(client);
    }

    /**
     * 获取自定义TokenSetting，TokenAuthType
     *
     * @return {@link TokenAuthType}
     */
    public TokenAuthType getTokenAuthType() {
        return Optional.<String>ofNullable(client.getTokenSettings()
                        .getSetting(ExtConfigurationSettingNames.TOKEN_AUTH_TYPE))
                .map(TokenAuthType::getEnum)
                .orElse(TokenAuthType.STANDARD);
    }

    /**
     * 设置 {@link TokenAuthType} 并返回新的 {@link RegisteredClient}
     *
     * @param tokenAuthType {@link TokenAuthType}
     * @return {@link RegisteredClient}
     */
    public RegisteredClient withTokenAuthType(TokenAuthType tokenAuthType) {
        Map<String, Object> settings = client.getTokenSettings().getSettings();
        return RegisteredClient.from(client)
                .tokenSettings(TokenSettings.withSettings(settings)
                        .setting(ExtConfigurationSettingNames.TOKEN_AUTH_TYPE, tokenAuthType.getValue())
                        .build())
                .build();
    }

    /**
     * 获取自定义ClientSetting，Status
     *
     * @return 客户端状态
     */
    public String getClientStatus() {
        return Optional.<String>of(client.getClientSettings()
                        .getSetting(ExtConfigurationSettingNames.CLIENT_STATUS))
                .orElse("");
    }

    /**
     * 设置 status 并返回新的 {@link RegisteredClient}
     *
     * @param status 客户端状态
     * @return {@link RegisteredClient}
     */
    public RegisteredClient withClientStatus(String status) {
        Map<String, Object> settings = client.getClientSettings().getSettings();
        return RegisteredClient.from(client)
                .clientSettings(ClientSettings.withSettings(settings)
                        .setting(ExtConfigurationSettingNames.CLIENT_STATUS, status)
                        .build())
                .build();
    }
}
