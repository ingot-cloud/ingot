package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleOauthClientMapper;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
public class SysRoleOauthClientServiceImpl extends CommonRoleRelationService<SysRoleOauthClientMapper, SysRoleOauthClient> implements SysRoleOauthClientService {

    private final Do remove = (roleId, targetId) -> remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
            .eq(SysRoleOauthClient::getRoleId, roleId)
            .eq(SysRoleOauthClient::getClientId, targetId));
    private final Do bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clientBindRoles(RelationDTO<Integer, Integer> params) {
        bindRoles(params, remove, bind,
                "SysRoleOauthClientServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindClients(RelationDTO<Integer, Integer> params) {
        bindTargets(params, remove, bind,
                "SysRoleOauthClientServiceImpl.RemoveFailed");
    }

    @Override
    public IPage<Oauth2RegisteredClient> getRoleClients(int roleId,
                                                       Page<?> page,
                                                       boolean isBind,
                                                       Oauth2RegisteredClient condition) {
        return getBaseMapper().getRoleClients(page, roleId, isBind, condition);
    }
}
