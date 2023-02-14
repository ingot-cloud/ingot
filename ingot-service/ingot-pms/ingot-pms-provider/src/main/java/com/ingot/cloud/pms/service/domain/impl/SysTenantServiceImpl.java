package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.mapper.SysTenantMapper;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SysTenantServiceImpl extends BaseServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {
    private final AssertionChecker assertI18nService;
    private final TenantProperties tenantProperties;

    @Override
    public IPage<SysTenant> conditionPage(Page<SysTenant> page, SysTenant params) {
        return page(page, Wrappers.lambdaQuery(params));
    }

    @Override
    public void createTenant(SysTenant params) {
        if (StrUtil.isNotEmpty(params.getCode())) {
            assertI18nService.checkOperation(count(Wrappers.<SysTenant>lambdaQuery()
                            .eq(SysTenant::getCode, params.getCode())) == 0,
                    "SysTenantServiceImpl.CodeExisted");
        }

        params.setCreatedAt(DateUtils.now());
        params.setStatus(CommonStatusEnum.ENABLE);
        assertI18nService.checkOperation(save(params),
                "SysTenantServiceImpl.CreateFailed");
    }

    @Override
    public void removeTenantById(long id) {
        assertI18nService.checkOperation(id != tenantProperties.getDefaultId(),
                "SysTenantServiceImpl.DefaultTenantRemoveFailed");

        assertI18nService.checkOperation(removeById(id),
                "SysTenantServiceImpl.RemoveFailed");
    }

    @Override
    public void updateTenantById(SysTenant params) {
        // 租户编码不可修改
        params.setCode(null);
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysTenantServiceImpl.UpdateFailed");
    }
}
