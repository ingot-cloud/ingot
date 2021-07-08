package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.mapper.SysOauthClientDetailsMapper;
import com.ingot.cloud.pms.service.domain.SysOauthClientDetailsService;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysOauthClientDetailsServiceImpl extends BaseServiceImpl<SysOauthClientDetailsMapper, SysOauthClientDetails> implements SysOauthClientDetailsService {
    private final SysRoleOauthClientService sysRoleOauthClientService;
    private final IdGenerator idGenerator;
    private final AssertI18nService assertI18nService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<SysOauthClientDetails> getClientsByRoles(List<Long> roleIds) {
        return baseMapper.getClientsByRoles(roleIds);
    }

    @Override
    public IPage<SysOauthClientDetails> conditionPage(Page<SysOauthClientDetails> page, SysOauthClientDetails condition) {
        return page(page, Wrappers.lambdaQuery(condition));
    }

    @Override
    public void createClient(SysOauthClientDetails params) {
        assertI18nService.checkOperation(count(Wrappers.<SysOauthClientDetails>lambdaQuery()
                        .eq(SysOauthClientDetails::getClientId, params.getClientId())) == 0,
                "SysOauthClientDetailsServiceImpl.ExistClientId");

        assertI18nService.checkOperation(count(Wrappers.<SysOauthClientDetails>lambdaQuery()
                        .eq(SysOauthClientDetails::getResourceId, params.getResourceId())) == 0,
                "SysOauthClientDetailsServiceImpl.ExistResourceId");

        params.setClientSecret(passwordEncoder.encode(params.getClientSecret()));
        params.setId(idGenerator.nextId());
        params.setCreatedAt(DateUtils.now());

        assertI18nService.checkOperation(save(params),
                "SysOauthClientDetailsServiceImpl.CreateFailed");
    }

    @Override
    @CacheEvict(value = CacheConstants.CLIENT_DETAILS_KEY, key = "#params.clientId")
    public void updateClientByClientId(SysOauthClientDetails params) {
        SysOauthClientDetails client = getClientByClientId(params.getClientId());

        // ClientId,ClientSecret,ResourceId 不可修改
        params.setId(client.getId());
        params.setClientId(null);
        params.setResourceId(null);
        params.setClientSecret(null);
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysOauthClientDetailsServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.CLIENT_DETAILS_KEY, key = "#clientId")
    public void removeClientByClientId(String clientId) {
        SysOauthClientDetails client = getClientByClientId(clientId);

        // 取消关联
        sysRoleOauthClientService.remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
                .eq(SysRoleOauthClient::getClientId, client.getId()));

        assertI18nService.checkOperation(removeById(client.getId()),
                "SysOauthClientDetailsServiceImpl.RemoveFailed");
    }

    private SysOauthClientDetails getClientByClientId(String clientId) {
        SysOauthClientDetails client = getOne(lambdaQuery()
                .eq(SysOauthClientDetails::getClientId, clientId));

        assertI18nService.checkOperation(client != null,
                "SysOauthClientDetailsServiceImpl.UpdateFailed");
        return client;
    }
}
