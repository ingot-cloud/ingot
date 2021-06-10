package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.mapper.SysTenantMapper;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.validation.service.AssertI18nService;
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
    private final AssertI18nService assertI18nService;

    @Override
    public IPage<SysTenant> conditionPage(Page<SysTenant> page, SysTenant params) {
        return page(page, Wrappers.lambdaQuery(params));
    }

    @Override
    public void createTenant(SysTenant params) {
        params.setCreatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysTenantServiceImpl.CreateFailed");
    }

    @Override
    public void removeTenantById(int id) {
        assertI18nService.checkOperation(removeById(id),
                "SysTenantServiceImpl.CreateFailed");
    }

    @Override
    public void updateTenantById(SysTenant params) {
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysTenantServiceImpl.CreateFailed");
    }
}
