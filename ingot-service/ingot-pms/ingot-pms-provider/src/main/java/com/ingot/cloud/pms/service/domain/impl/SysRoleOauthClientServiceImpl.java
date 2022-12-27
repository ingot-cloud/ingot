package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleOauthClientMapper;
import com.ingot.cloud.pms.service.domain.SysRoleOauthClientService;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class SysRoleOauthClientServiceImpl extends CommonRoleRelationService<SysRoleOauthClientMapper, SysRoleOauthClient, String> implements SysRoleOauthClientService {

    private final Do<String> remove = (roleId, targetId) -> remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
            .eq(SysRoleOauthClient::getRoleId, roleId)
            .eq(SysRoleOauthClient::getClientId, targetId));
    private final Do<String> bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = "'role-*'")
    public void clientBindRoles(RelationDTO<String, Long> params) {
        bindRoles(params, remove, bind,
                "SysRoleOauthClientServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.CLIENT_DETAILS, key = "'role-*'")
    public void roleBindClients(RelationDTO<Long, String> params) {
        bindTargets(params, remove, bind,
                "SysRoleOauthClientServiceImpl.RemoveFailed");
    }

    @Override
    @Cacheable(value = CacheConstants.CLIENT_DETAILS, key = "'role-' + #roleId", unless = "#result.isEmpty()")
    public List<Oauth2RegisteredClient> getRoleClients(long roleId) {
        return CollUtil.emptyIfNull(baseMapper.getClientsByRole(roleId));
    }

    @Override
    public List<Oauth2RegisteredClient> getRoleClients(long roleId,
                                                       Oauth2RegisteredClient condition) {
        List<Oauth2RegisteredClient> list = SpringContextHolder
                .getBean(SysRoleOauthClientService.class).getRoleClients(roleId);
        return list.stream()
                .filter(BizFilter.clientFilter(condition))
                .sorted(Comparator.comparing(Oauth2RegisteredClient::getClientIdIssuedAt))
                .collect(Collectors.toList());
    }
}
