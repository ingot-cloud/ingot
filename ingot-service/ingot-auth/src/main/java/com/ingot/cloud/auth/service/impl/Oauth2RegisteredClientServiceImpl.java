package com.ingot.cloud.auth.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.common.BizFilter;
import com.ingot.cloud.auth.common.CacheKey;
import com.ingot.cloud.auth.mapper.Oauth2RegisteredClientMapper;
import com.ingot.cloud.auth.model.convert.ClientConvert;
import com.ingot.cloud.auth.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.auth.model.dto.OAuth2RegisteredClientDTO;
import com.ingot.cloud.auth.model.vo.AppSecretVO;
import com.ingot.cloud.auth.model.vo.OAuth2RegisteredClientVO;
import com.ingot.cloud.auth.service.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.api.rpc.RemotePmsIdService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.client.RegisteredClientOps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final ClientConvert clientConvert;
    private final RemotePmsIdService remotePmsIdService;

    @Override
    @Cacheable(value = CacheConstants.CLIENT_DETAILS, key = CacheKey.ClientListKey, unless = "#result.isEmpty()")
    public List<Oauth2RegisteredClient> list() {
        return super.list();
    }

    @Override
    public List<Oauth2RegisteredClient> list(Oauth2RegisteredClient condition) {
        return SpringContextHolder.getBean(Oauth2RegisteredClientService.class)
                .list().stream()
                .filter(BizFilter.clientFilter(condition)).collect(Collectors.toList());
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
    @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = CacheKey.ClientListKey)
    public AppSecretVO createClient(OAuth2RegisteredClientDTO params) {
        assertI18nService.checkOperation(count(Wrappers.<Oauth2RegisteredClient>lambdaQuery()
                        .eq(Oauth2RegisteredClient::getClientId, params.getClientId())) == 0,
                "Oauth2RegisteredClientServiceImpl.ExistClientId");

        Oauth2RegisteredClient client = clientConvert.to(params);
        // id 和 clientId 保持一致
        String id = remotePmsIdService.genAppId()
                .ifError(OAuth2ErrorUtils::checkResponse)
                .getData();
        String secret = StrUtil.uuid().replaceAll("-", "");
        AppSecretVO result = new AppSecretVO();
        result.setAppId(id);
        result.setAppSecret(secret);

        client.setId(id);
        client.setClientId(id);
        client.setClientIdIssuedAt(DateUtil.now());
        client.setClientSecret(passwordEncoder.encode(secret));

        ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
        params.setStatus(CommonStatusEnum.ENABLE);
        if (StrUtil.isEmpty(params.getTokenAuthType())) {
            params.setTokenAuthType(TokenAuthTypeEnum.STANDARD.getValue());
        }
        fillSettings(params, clientSettingsBuilder, tokenSettingsBuilder);

        client.setClientSettings(clientSettingsBuilder.build());
        client.setTokenSettings(tokenSettingsBuilder.build());
        client.setUpdatedAt(DateUtil.now());

        assertI18nService.checkOperation(save(client),
                "Oauth2RegisteredClientServiceImpl.CreateFailed");

        return result;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = "#params.id"),
            @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = CacheKey.ClientListKey)
    })
    public void updateClientByClientId(OAuth2RegisteredClientDTO params) {
        Oauth2RegisteredClient current = getById(params.getId());
        ClientSettings.Builder clientSettingsBuilder =
                ClientSettings.withSettings(current.getClientSettings().getSettings());
        TokenSettings.Builder tokenSettingsBuilder =
                TokenSettings.withSettings(current.getTokenSettings().getSettings());
        fillSettings(params, clientSettingsBuilder, tokenSettingsBuilder);

        Oauth2RegisteredClient client = clientConvert.to(params);
        client.setClientSecret(null);
        client.setClientSettings(clientSettingsBuilder.build());
        client.setTokenSettings(tokenSettingsBuilder.build());
        client.setUpdatedAt(DateUtil.now());

        assertI18nService.checkOperation(updateById(client),
                "Oauth2RegisteredClientServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = "#id"),
            @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = CacheKey.ClientListKey)
    })
    public void removeClientByClientId(String id) {
        assertI18nService.checkOperation(removeById(id),
                "Oauth2RegisteredClientServiceImpl.RemoveFailed");
    }

    @Override
    public AppSecretVO resetSecret(String id) {
        Oauth2RegisteredClient current = getById(id);
        String secret = StrUtil.uuid().replaceAll("-", "");
        AppSecretVO result = new AppSecretVO();
        result.setAppId(id);
        result.setAppSecret(secret);

        current.setClientSecret(passwordEncoder.encode(secret));
        current.setUpdatedAt(DateUtil.now());
        current.updateById();
        return result;
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
            TokenAuthTypeEnum type = TokenAuthTypeEnum.getEnum(params.getTokenAuthType());
            if (type != null) {
                RegisteredClientOps.setTokenAuthType(tokenSettingsBuilder, type);
            }
        }
    }

    private OAuth2RegisteredClientVO toVo(Oauth2RegisteredClient client) {
        OAuth2RegisteredClientVO item = clientConvert.to(client);
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
