package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDto;
import com.ingot.cloud.pms.mapper.Oauth2RegisteredClientMapper;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
@Service
public class Oauth2RegisteredClientServiceImpl extends BaseServiceImpl<Oauth2RegisteredClientMapper, Oauth2RegisteredClient>
        implements Oauth2RegisteredClientService {

    @Override
    public List<Oauth2RegisteredClient> getClientsByRoles(List<Long> roleIds) {
        return getBaseMapper().getClientsByRoles(roleIds);
    }

    @Override
    public IPage<Oauth2RegisteredClient> conditionPage(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        return page(page, Wrappers.lambdaQuery(condition));
    }

    @Override
    public void createClient(OAuth2RegisteredClientDto params) {
        // todo
    }

    @Override
    public void updateClientByClientId(OAuth2RegisteredClientDto params) {

    }

    @Override
    public void removeClientByClientId(String clientId) {

    }
}
