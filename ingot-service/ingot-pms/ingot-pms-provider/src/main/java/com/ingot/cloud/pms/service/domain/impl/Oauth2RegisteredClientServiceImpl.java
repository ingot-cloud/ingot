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
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDto;
import com.ingot.cloud.pms.api.model.transform.ClientTrans;
import com.ingot.cloud.pms.api.model.vo.client.OAuth2RegisteredClientVo;
import com.ingot.cloud.pms.mapper.Oauth2RegisteredClientMapper;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.constants.RedisConstants;
import com.ingot.framework.core.validation.service.AssertI18nService;
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
    private final AssertI18nService assertI18nService;
    private final PasswordEncoder passwordEncoder;
    private final ClientTrans clientTrans;

    private final SysRoleOauthClientService sysRoleOauthClientService;

    @Override
    public List<Oauth2RegisteredClient> getClientsByRoles(List<Long> roleIds) {
        return getBaseMapper().getClientsByRoles(roleIds);
    }

    @Override
    public IPage<OAuth2RegisteredClientVo> conditionPage(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        IPage<Oauth2RegisteredClient> tmp = page(page, Wrappers.lambdaQuery(condition));

        List<OAuth2RegisteredClientVo> list = tmp.getRecords()
                .stream().map(this::toVo).collect(Collectors.toList());

        IPage<OAuth2RegisteredClientVo> result = new Page<>();
        result.setCurrent(tmp.getCurrent());
        result.setPages(tmp.getPages());
        result.setSize(tmp.getSize());
        result.setTotal(tmp.getTotal());
        result.setRecords(list);
        return result;
    }

    @Override
    public OAuth2RegisteredClientVo getByClientId(String clientId) {
        Oauth2RegisteredClient current;
        OAuth2RegisteredClientVo result = toVo(current = getById(clientId));
        result.setClientSecret(current.getClientSecret());
        return result;
    }

    @Override
    public void createClient(OAuth2RegisteredClientDto params) {
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
        fillSettings(params, clientSettingsBuilder, tokenSettingsBuilder);

        client.setClientSettings(clientSettingsBuilder.build());
        client.setTokenSettings(tokenSettingsBuilder.build());
        client.setUpdatedAt(DateUtils.now());

        assertI18nService.checkOperation(save(client),
                "Oauth2RegisteredClientServiceImpl.CreateFailed");
    }

    @Override
    @CacheEvict(value = RedisConstants.Cache.REGISTERED_CLIENT_KEY, key = "#params.clientId")
    public void updateClientByClientId(OAuth2RegisteredClientDto params) {
        Oauth2RegisteredClient current = getById(params.getClientId());
        ClientSettings.Builder clientSettingsBuilder =
                ClientSettings.withSettings(current.getClientSettings().getSettings());
        TokenSettings.Builder tokenSettingsBuilder =
                TokenSettings.withSettings(current.getTokenSettings().getSettings());
        fillSettings(params, clientSettingsBuilder, tokenSettingsBuilder);

        Oauth2RegisteredClient client = clientTrans.to(params);
        client.setId(client.getClientId());
        client.setClientSecret(null);
        client.setClientSettings(clientSettingsBuilder.build());
        client.setTokenSettings(tokenSettingsBuilder.build());
        client.setUpdatedAt(DateUtils.now());

        assertI18nService.checkOperation(updateById(client),
                "Oauth2RegisteredClientServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisConstants.Cache.REGISTERED_CLIENT_KEY, key = "#clientId")
    public void removeClientByClientId(String clientId) {
        // 取消关联
        sysRoleOauthClientService.remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
                .eq(SysRoleOauthClient::getClientId, clientId));

        assertI18nService.checkOperation(removeById(clientId),
                "Oauth2RegisteredClientServiceImpl.RemoveFailed");
    }

    private void fillSettings(OAuth2RegisteredClientDto params,
                              ClientSettings.Builder clientSettingsBuilder,
                              TokenSettings.Builder tokenSettingsBuilder) {
        if (params.getRequireAuthorizationConsent() != null) {
            clientSettingsBuilder.requireAuthorizationConsent(params.getRequireAuthorizationConsent());
        }
        if (params.getRequireProofKey() != null) {
            clientSettingsBuilder.requireProofKey(params.getRequireProofKey());
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
    }

    private OAuth2RegisteredClientVo toVo(Oauth2RegisteredClient client) {
        OAuth2RegisteredClientVo item = clientTrans.to(client);
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
        return item;
    }
}
