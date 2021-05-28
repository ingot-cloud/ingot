package com.ingot.cloud.pms.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.cloud.pms.mapper.SysOauthClientDetailsMapper;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.service.SysOauthClientDetailsService;
import com.ingot.cloud.pms.service.SysRoleOauthClientService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class SysOauthClientDetailsServiceImpl extends BaseServiceImpl<SysOauthClientDetailsMapper, SysOauthClientDetails> implements SysOauthClientDetailsService {
    private final SysRoleOauthClientService sysRoleOauthClientService;
    private final IdGenerator idGenerator;
    private final I18nService i18nService;

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
        AssertionUtils.checkOperation(count(Wrappers.<SysOauthClientDetails>lambdaQuery()
                        .eq(SysOauthClientDetails::getClientId, params.getClientId())) == 0,
                i18nService.getMessage("SysOauthClientDetailsServiceImpl.ExistClientId"));

        AssertionUtils.checkOperation(count(Wrappers.<SysOauthClientDetails>lambdaQuery()
                        .eq(SysOauthClientDetails::getResourceId, params.getResourceId())) == 0,
                i18nService.getMessage("SysOauthClientDetailsServiceImpl.ExistResourceId"));

        params.setId(idGenerator.nextId());
        params.setCreatedAt(DateUtils.now());

        AssertionUtils.checkOperation(save(params),
                i18nService.getMessage("SysOauthClientDetailsServiceImpl.CreateFailed"));
    }

    @Override
    public void updateClient(SysOauthClientDetails params) {
        // ClientId,ResourceId 不可修改
        params.setClientId(null);
        params.setResourceId(null);
        params.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(params),
                i18nService.getMessage("SysOauthClientDetailsServiceImpl.UpdateFailed"));
    }

    @Override
    public void removeClientById(long id) {
        // 取消关联
        sysRoleOauthClientService.remove(Wrappers.<SysRoleOauthClient>lambdaQuery()
                .eq(SysRoleOauthClient::getClientId, id));

        AssertionUtils.checkOperation(removeById(id),
                i18nService.getMessage("SysOauthClientDetailsServiceImpl.RemoveFailed"));
    }
}
