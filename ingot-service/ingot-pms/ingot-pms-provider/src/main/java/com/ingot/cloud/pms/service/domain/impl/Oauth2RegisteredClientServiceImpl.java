package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
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
}
