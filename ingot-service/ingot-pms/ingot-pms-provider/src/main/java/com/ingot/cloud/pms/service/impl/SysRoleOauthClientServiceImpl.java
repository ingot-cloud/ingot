package com.ingot.cloud.pms.service.impl;

import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.mapper.SysRoleOauthClientMapper;
import com.ingot.cloud.pms.service.SysRoleOauthClientService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
public class SysRoleOauthClientServiceImpl extends BaseServiceImpl<SysRoleOauthClientMapper, SysRoleOauthClient> implements SysRoleOauthClientService {

    @Override
    public void clientBindRoles(RelationDto<Long, Long> params) {

    }

    @Override
    public void roleBindClients(RelationDto<Long, Long> params) {

    }
}
