package com.ingot.cloud.pms.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.mapper.SysTenantMapper;
import com.ingot.cloud.pms.service.SysTenantService;
import com.ingot.framework.base.utils.DateUtils;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class SysTenantServiceImpl extends BaseServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {
    private final I18nService i18nService;

    @Override
    public IPage<SysTenant> conditionPage(Page<SysTenant> page, SysTenant params) {
        return page(page, Wrappers.lambdaQuery(params));
    }

    @Override
    public void createTenant(SysTenant params) {
        params.setCreatedAt(DateUtils.now());
        AssertionUtils.checkOperation(save(params),
                i18nService.getMessage("SysTenantServiceImpl.CreateFailed"));
    }

    @Override
    public void removeTenantById(long id) {
        AssertionUtils.checkOperation(removeById(id),
                i18nService.getMessage("SysTenantServiceImpl.CreateFailed"));
    }

    @Override
    public void updateTenantById(SysTenant params) {
        params.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(params),
                i18nService.getMessage("SysTenantServiceImpl.CreateFailed"));
    }
}
