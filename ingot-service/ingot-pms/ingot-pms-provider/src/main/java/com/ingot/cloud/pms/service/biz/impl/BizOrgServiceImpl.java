package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.Objects;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.domain.TenantAppConfig;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.app.AppEnabledDTO;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.core.BizAuthorityUtils;
import com.ingot.cloud.pms.core.TenantEngine;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizOrgService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.OrgConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizOrgServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizOrgServiceImpl implements BizOrgService {
    private final TenantEngine tenantEngine;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;
    private final AppUserTenantService appUserTenantService;
    private final AssertionChecker assertionChecker;

    private final MetaAppService metaAppService;
    private final MetaAuthorityService metaAuthorityService;
    private final TenantAppConfigService tenantAppConfigService;

    private final BizAppService bizAppService;
    private final AuthorityConvert authorityConvert;

    @Override
    public IPage<SysTenant> conditionPage(Page<SysTenant> page, SysTenant params) {
        return sysTenantService.conditionPage(page, params);
    }

    @Override
    public List<AuthorityTreeNodeVO> getTenantAuthorityTree(long tenantID) {
        return TenantEnv.applyAs(tenantID, () -> {
            List<AuthorityTreeNodeVO> authorities = BizAuthorityUtils.getTenantAuthorities(
                    tenantID, bizAppService, metaAuthorityService, authorityConvert);
            return TreeUtil.build(authorities);
        });
    }

    @Override
    public List<SysTenant> search(SysTenant filter) {
        String name = filter.getName();
        if (StrUtil.isEmpty(name)) {
            return ListUtil.empty();
        }

        return CollUtil.emptyIfNull(
                sysTenantService.list(Wrappers.<SysTenant>lambdaQuery()
                        .eq(SysTenant::getStatus, CommonStatusEnum.ENABLE)
                        .like(SysTenant::getName, name)));
    }

    @Override
    public SysTenant getDetails(long id) {
        return sysTenantService.getById(id);
    }

    @Override
    public List<MetaApp> getOrgApps(long tenantId) {
        return TenantEnv.applyAs(tenantId, () -> {
            List<MetaApp> list = metaAppService.list();
            if (CollUtil.isEmpty(list)) {
                return ListUtil.empty();
            }

            List<TenantAppConfig> appConfigs = tenantAppConfigService.list();

            return list.stream().peek(item ->
                    appConfigs.stream()
                            .filter(config -> Objects.equals(config.getMetaId(), item.getId()))
                            .findFirst()
                            .ifPresent(config ->
                                    item.setStatus(config.getEnabled()
                                            ? CommonStatusEnum.ENABLE : CommonStatusEnum.LOCK))).toList();
        });
    }

    @Override
    public void updateOrgAppStatus(AppEnabledDTO params) {
        TenantAppConfig tenantAppConfig = tenantAppConfigService.getOne(Wrappers.<TenantAppConfig>lambdaQuery()
                .eq(TenantAppConfig::getMetaId, params.getId()));
        if (tenantAppConfig == null) {
            tenantAppConfig = new TenantAppConfig();
            tenantAppConfig.setMetaId(params.getId());
            tenantAppConfig.setEnabled(params.getEnabled());
            tenantAppConfigService.create(tenantAppConfig);
            return;
        }

        tenantAppConfig.setEnabled(params.getEnabled());
        tenantAppConfigService.update(tenantAppConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrg(CreateOrgDTO params) {
        // 1. 创建tenant
        SysTenant tenant = tenantEngine.createTenant(params);

        // 2. 创建部门
        TenantDept dept = tenantEngine.createTenantDept(tenant);

        // 3. 初始化组织管理员
        tenantEngine.initTenantManager(params, tenant, dept);
    }

    @Override
    public void updateBase(SysTenant params) {
        if (params.getStatus() != null && params.getStatus() == CommonStatusEnum.LOCK) {
            SysTenant org = sysTenantService.getById(params.getId());
            String code = org.getCode();
            // 平台默认组织不可更新状态
            if (StrUtil.equals(code, OrgConstants.INGOT_CLOUD_CODE)) {
                assertionChecker.checkOperation(!StrUtil.equals(code, OrgConstants.INGOT_CLOUD_CODE),
                        "Platform.canNotDisableIngotOrg");
            }
        }

        params.setEndAt(null);
        sysTenantService.updateTenantById(params);

        if (StrUtil.isNotEmpty(params.getName())) {
            sysUserTenantService.updateBase(params);
            appUserTenantService.updateBase(params);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrg(long id) {
        SysTenant org = sysTenantService.getById(id);
        String code = org.getCode();
        assertionChecker.checkOperation(!StrUtil.equals(code, OrgConstants.INGOT_CLOUD_CODE), "Platform.canNotRemoveIngotOrg");

        // 销毁组织
        TenantEnv.runAs(id, () -> tenantEngine.destroy(id));
    }
}
