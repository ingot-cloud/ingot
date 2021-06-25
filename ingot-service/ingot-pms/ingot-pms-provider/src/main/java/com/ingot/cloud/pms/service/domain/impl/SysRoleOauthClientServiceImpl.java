package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleOauthClientMapper;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import org.springframework.stereotype.Service;

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

    @Override
    public void clientBindRoles(RelationDto<Long, Long> params) {
        bindRoles(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
                        .eq(SysRoleOauthClient::getRoleId, roleId)
                        .eq(SysRoleOauthClient::getClientId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleOauthClientServiceImpl.RemoveFailed");
    }

    @Override
    public void roleBindClients(RelationDto<Long, Long> params) {
        bindTargets(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
                        .eq(SysRoleOauthClient::getRoleId, roleId)
                        .eq(SysRoleOauthClient::getClientId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleOauthClientServiceImpl.RemoveFailed");
    }

    @Override
    public IPage<SysOauthClientDetails> getRoleBindClients(long roleId, Page<?> page) {
        return getBaseMapper().getRoleBindClients(page, roleId);
    }
}
