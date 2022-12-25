package com.ingot.cloud.pms.service.domain.impl;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDTO;
import com.ingot.cloud.pms.api.model.transform.ClientTrans;
import com.ingot.cloud.pms.api.model.vo.client.OAuth2RegisteredClientVO;
import com.ingot.cloud.pms.mapper.Oauth2RegisteredClientMapper;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.constants.RedisConstants;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.TokenAuthType;
import com.ingot.framework.security.oauth2.server.authorization.client.RegisteredClientOps;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Oauth2RegisteredClientServiceImpl extends BaseServiceImpl<Oauth2RegisteredClientMapper, Oauth2RegisteredClient>
        implements Oauth2RegisteredClientService {
    private final AssertionChecker assertI18nService;
    private final PasswordEncoder passwordEncoder;
    private final ClientTrans clientTrans;

    private final SysRoleOauthClientService sysRoleOauthClientService;

    @Override
    public List<Oauth2RegisteredClient> getClientsByRoles(List<Long> roleIds) {
        List<String> clientIds = sysRoleOauthClientService.list(Wrappers.<SysRoleOauthClient>lambdaQuery()
                        .in(SysRoleOauthClient::getRoleId, roleIds))
                .stream()
                .map(SysRoleOauthClient::getClientId)
                .collect(Collectors.toList());
        return list(Wrappers.<Oauth2RegisteredClient>lambdaQuery()
                .in(Oauth2RegisteredClient::getId, clientIds))
                .stream()
                .filter(client -> {
                    String status = RegisteredClientOps.getClientStatus(client.getClientSettings());
                    return CommonStatusEnum.getEnum(status) == CommonStatusEnum.ENABLE;
                }).collect(Collectors.toList());
    }

    @Override
    public IPage<OAuth2RegisteredClientVO> conditionPage(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        IPage<Oauth2RegisteredClient> tmp = page(page, Wrappers.lambdaQuery(condition));

        List<OAuth2RegisteredClientVO> list = tmp.getRecords()
                .stream().map(this::toVo).collect(Collectors.toList());

        IPage<OAuth2RegisteredClientVO> result = new Page<>();
        result.setCurrent(tmp.getCurrent());
        result.setPages(tmp.getPages());
        result.setSize(tmp.getSize());
        result.setTotal(tmp.getTotal());
        result.setRecords(list);
        return result;
    }

    @Override
    public OAuth2RegisteredClientVO getByClientId(String id) {
        Oauth2RegisteredClient current;
        OAuth2RegisteredClientVO result = toVo(current = getById(id));
        result.setClientSecret(current.getClientSecret());
        return result;
    }

    @Override
    public void createClient(OAuth2RegisteredClientDTO params) {
        assertI18nService.checkOperation(count(Wrappers.<Oauth2RegisteredClient>lambdaQuery()
                        .eq(Oauth2RegisteredClient::getClientId, params.getClientId())) == 0,
                "Oauth2RegisteredClientServiceImpl.ExistClientId");

        Oauth2RegisteredClient client = clientTrans.to(params);
        // id 和 clientId 保持一致
        client.setId(client.getClientId());
        client.setClientIdIssuedAt(DateUtils.now());
        client.setClientSecret(passwordEncoder.encode(client.getClientSecret()));

        ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
        params.setStatus(CommonStatusEnum.ENABLE);
        if (StrUtil.isEmpty(params.getTokenAuthType())) {
            params.setTokenAuthType(TokenAuthType.STANDARD.getValue());
        }
        fillSettings(params, clientSettingsBuilder, tokenSettingsBuilder);

        client.setClientSettings(clientSettingsBuilder.build());
        client.setTokenSettings(tokenSettingsBuilder.build());
        client.setUpdatedAt(DateUtils.now());

        assertI18nService.checkOperation(save(client),
                "Oauth2RegisteredClientServiceImpl.CreateFailed");
    }

    @Override
    @CacheEvict(value = RedisConstants.Cache.REGISTERED_CLIENT_KEY_ID, key = "#params.id")
    public void updateClientByClientId(OAuth2RegisteredClientDTO params) {
        Oauth2RegisteredClient current = getById(params.getId());
        ClientSettings.Builder clientSettingsBuilder =
                ClientSettings.withSettings(current.getClientSettings().getSettings());
        TokenSettings.Builder tokenSettingsBuilder =
                TokenSettings.withSettings(current.getTokenSettings().getSettings());
        fillSettings(params, clientSettingsBuilder, tokenSettingsBuilder);

        Oauth2RegisteredClient client = clientTrans.to(params);
        client.setClientSecret(null);
        client.setClientSettings(clientSettingsBuilder.build());
        client.setTokenSettings(tokenSettingsBuilder.build());
        client.setUpdatedAt(DateUtils.now());

        assertI18nService.checkOperation(updateById(client),
                "Oauth2RegisteredClientServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisConstants.Cache.REGISTERED_CLIENT_KEY_ID, key = "#id")
    public void removeClientByClientId(String id) {
        // 取消关联
        sysRoleOauthClientService.remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
                .eq(SysRoleOauthClient::getClientId, id));

        assertI18nService.checkOperation(removeById(id),
                "Oauth2RegisteredClientServiceImpl.RemoveFailed");
    }

    private void fillSettings(OAuth2RegisteredClientDTO params,
                              ClientSettings.Builder clientSettingsBuilder,
                              TokenSettings.Builder tokenSettingsBuilder) {
        if (params.getRequireAuthorizationConsent() != null) {
            clientSettingsBuilder.requireAuthorizationConsent(params.getRequireAuthorizationConsent());
        }
        if (params.getRequireProofKey() != null) {
            clientSettingsBuilder.requireProofKey(params.getRequireProofKey());
        }
        if (params.getStatus() != null) {
            RegisteredClientOps.setClientStatus(clientSettingsBuilder, params.getStatus().getValue());
        }

        if (StrUtil.isNotEmpty(params.getAuthorizationCodeTimeToLive())) {
            tokenSettingsBuilder.authorizationCodeTimeToLive(
                    Duration.ofSeconds(Long.parseLong(params.getAuthorizationCodeTimeToLive())));
        }
        if (StrUtil.isNotEmpty(params.getAccessTokenTimeToLive())) {
            tokenSettingsBuilder.accessTokenTimeToLive(
                    Duration.ofSeconds(Long.parseLong(params.getAccessTokenTimeToLive())));
        }
        if (StrUtil.isNotEmpty(params.getRefreshTokenTimeToLive())) {
            tokenSettingsBuilder.refreshTokenTimeToLive(
                    Duration.ofSeconds(Long.parseLong(params.getRefreshTokenTimeToLive())));
        }
        if (params.getReuseRefreshTokens() != null) {
            tokenSettingsBuilder.reuseRefreshTokens(params.getReuseRefreshTokens());
        }
        if (StrUtil.isNotEmpty(params.getIdTokenSignatureAlgorithm())) {
            tokenSettingsBuilder.idTokenSignatureAlgorithm(
                    SignatureAlgorithm.from(params.getIdTokenSignatureAlgorithm()));
        }
        if (StrUtil.isNotEmpty(params.getTokenAuthType())) {
            TokenAuthType type = TokenAuthType.getEnum(params.getTokenAuthType());
            if (type != null) {
                RegisteredClientOps.setTokenAuthType(tokenSettingsBuilder, type);
            }
        }
    }

    private OAuth2RegisteredClientVO toVo(Oauth2RegisteredClient client) {
        OAuth2RegisteredClientVO item = clientTrans.to(client);
        // 覆盖秘钥为null
        item.setClientSecret(null);
        item.setRequireAuthorizationConsent(client.getClientSettings().isRequireAuthorizationConsent());
        item.setRequireProofKey(client.getClientSettings().isRequireProofKey());
        item.setAccessTokenTimeToLive(
                String.valueOf(client.getTokenSettings().getAccessTokenTimeToLive().getSeconds()));
        item.setRefreshTokenTimeToLive(
                String.valueOf(client.getTokenSettings().getRefreshTokenTimeToLive().getSeconds()));
        item.setReuseRefreshTokens(client.getTokenSettings().isReuseRefreshTokens());
        item.setIdTokenSignatureAlgorithm(
                client.getTokenSettings().getIdTokenSignatureAlgorithm().getName());

        item.setTokenAuthType(
                RegisteredClientOps.getTokenAuthType(client.getTokenSettings()).getValue());
        item.setStatus(
                CommonStatusEnum.getEnum(RegisteredClientOps.getClientStatus(client.getClientSettings())));
        return item;
    }
}
