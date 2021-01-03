package com.ingot.cloud.pms.service.impl;

import com.ingot.cloud.pms.mapper.SysOauthClientDetailsMapper;
import com.ingot.cloud.pms.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.service.SysOauthClientDetailsService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

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
public class SysOauthClientDetailsServiceImpl extends BaseServiceImpl<SysOauthClientDetailsMapper, SysOauthClientDetails> implements SysOauthClientDetailsService {

    @Override
    public List<SysOauthClientDetails> getClientsByRoles(List<Long> roleIds) {
        return baseMapper.getClientsByRoles(roleIds);
    }
}
