package com.ingot.framework.security.oauth2.server.authorization.client;

import java.util.Map;
import java.util.Optional;

import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
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
     * @param settings {@link TokenSettings}
     * @return {@link TokenAuthTypeEnum}
     */
    public static TokenAuthTypeEnum getTokenAuthType(TokenSettings settings) {
        return Optional.<String>ofNullable(settings.getSetting(ExtConfigurationSettingNames.TOKEN_AUTH_TYPE))
                .map(TokenAuthTypeEnum::getEnum)
                .orElse(TokenAuthTypeEnum.STANDARD);
    }

    /**
     * {@link TokenSettings.Builder} 设置 {@link TokenAuthTypeEnum}
     *
     * @param builder       {@link TokenSettings.Builder}
     * @param tokenAuthType {@link TokenAuthTypeEnum}
     * @return {@link TokenSettings.Builder}
     */
    public static TokenSettings.Builder setTokenAuthType(TokenSettings.Builder builder, TokenAuthTypeEnum tokenAuthType) {
        builder.setting(ExtConfigurationSettingNames.TOKEN_AUTH_TYPE, tokenAuthType.getValue());
        return builder;
    }

    /**
     * 给 {@link TokenSettings} 设置 {@link TokenAuthTypeEnum}
     *
     * @param tokenSettings {@link TokenSettings}
     * @param tokenAuthType {@link TokenAuthTypeEnum}
     * @return {@link TokenSettings}
     */
    public static TokenSettings setTokenAuthType(TokenSettings tokenSettings, TokenAuthTypeEnum tokenAuthType) {
        Map<String, Object> settings = tokenSettings.getSettings();
        return setTokenAuthType(TokenSettings.withSettings(settings), tokenAuthType).build();
    }

    /**
     * 获取自定义ClientSetting，Status
     *
     * @param settings {@link ClientSettings}
     * @return 客户端状态
     */
    public static String getClientStatus(ClientSettings settings) {
        return Optional.<String>of(settings.getSetting(ExtConfigurationSettingNames.CLIENT_STATUS))
                .orElse("");
    }

    /**
     * {@link ClientSettings.Builder} 设置状态
     *
     * @param builder {@link ClientSettings.Builder}
     * @param status  状态
     * @return {@link ClientSettings.Builder}
     */
    public static ClientSettings.Builder setClientStatus(ClientSettings.Builder builder, String status) {
        builder.setting(ExtConfigurationSettingNames.CLIENT_STATUS, status);
        return builder;
    }

    /**
     * 给 {@link ClientSettings} 设置状态
     *
     * @param clientSettings {@link ClientSettings}
     * @param status         状态
     * @return {@link ClientSettings}
     */
    public static ClientSettings setClientStatus(ClientSettings clientSettings, String status) {
        Map<String, Object> settings = clientSettings.getSettings();
        return setClientStatus(ClientSettings.withSettings(settings), status).build();
    }

    /**
     * 获取自定义TokenSetting，TokenAuthType
     *
     * @return {@link TokenAuthTypeEnum}
     */
    public TokenAuthTypeEnum getTokenAuthType() {
        return getTokenAuthType(client.getTokenSettings());
    }

    /**
     * 设置 {@link TokenAuthTypeEnum} 并返回新的 {@link RegisteredClient}
     *
     * @param tokenAuthType {@link TokenAuthTypeEnum}
     * @return {@link RegisteredClient}
     */
    public RegisteredClient withTokenAuthType(TokenAuthTypeEnum tokenAuthType) {
        return RegisteredClient.from(client)
                .tokenSettings(setTokenAuthType(client.getTokenSettings(), tokenAuthType))
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
                .clientSettings(setClientStatus(client.getClientSettings(), status))
                .build();
    }
}
