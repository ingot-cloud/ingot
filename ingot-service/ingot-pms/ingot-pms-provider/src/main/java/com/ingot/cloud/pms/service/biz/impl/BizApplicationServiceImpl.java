package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.dto.application.ApplicationFilterDTO;
import com.ingot.cloud.pms.api.model.transform.ApplicationTrans;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationOrgPageItemVO;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationPageItemVO;
import com.ingot.cloud.pms.core.org.TenantOps;
import com.ingot.cloud.pms.service.biz.BizApplicationService;
import com.ingot.cloud.pms.service.domain.SysApplicationService;
import com.ingot.cloud.pms.service.domain.SysApplicationTenantService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * <p>Description  : BizApplicationServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 10:21.</p>
 */
@Service
@RequiredArgsConstructor
public class BizApplicationServiceImpl implements BizApplicationService {
    private final SysApplicationService sysApplicationService;
    private final SysApplicationTenantService sysApplicationTenantService;
    private final SysMenuService sysMenuService;

    private final TenantOps tenantOps;
    private final AssertionChecker assertionChecker;
    private final ApplicationTrans applicationTrans;

    @Override
    public IPage<ApplicationPageItemVO> page(Page<SysApplication> page, ApplicationFilterDTO filter) {
        return sysApplicationService.page(page, filter);
    }

    @Override
    public List<ApplicationOrgPageItemVO> orgApplicationList(long orgId) {
        return TenantEnv.applyAs(orgId, () -> {
            List<SysApplicationTenant> list = sysApplicationTenantService.list();
            if (CollUtil.isEmpty(list)) {
                return ListUtil.empty();
            }

            List<SysMenu> menuList = sysMenuService.list(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getId, list.stream().map(SysApplicationTenant::getMenuId).toList()));

            return list.stream().map(item -> {
                ApplicationOrgPageItemVO pageItem = applicationTrans.to(item);
                menuList.stream()
                        .filter(menuItem -> Objects.equals(menuItem.getId(), item.getMenuId()))
                        .findFirst()
                        .ifPresent(menuItem -> {
                            pageItem.setMenuIcon(menuItem.getIcon());
                            pageItem.setMenuName(menuItem.getName());
                        });
                return pageItem;
            }).toList();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createApplication(SysApplication params) {
        assertionChecker.checkOperation(params.getMenuId() != null, "BizApplicationServiceImpl.MenuCantNull");
        assertionChecker.checkOperation(params.getAuthorityId() != null, "BizApplicationServiceImpl.AuthorityCantNull");

        params.setStatus(CommonStatusEnum.ENABLE);
        params.setCreatedAt(DateUtils.now());
        sysApplicationService.save(params);

        tenantOps.createApplication(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApplicationStatus(SysApplication params) {
        SysApplication application = sysApplicationService.getById(params.getId());
        // 1.如果当前是默认应用，更新状态需要同步所有组织
        if (BooleanUtil.isTrue(application.getDefaultApp())) {
            tenantOps.updateApplication(application, params.getStatus());
        }

        // 2.如果当前不是默认应用，更新状态不用同步所有组织
        SysApplication entity = new SysApplication();
        entity.setId(params.getId());
        entity.setStatus(params.getStatus());
        sysApplicationService.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApplicationDefault(SysApplication params) {
        SysApplication application = sysApplicationService.getById(params.getId());

        SysApplication entity = new SysApplication();
        entity.setId(params.getId());
        entity.setDefaultApp(params.getDefaultApp());
        sysApplicationService.updateById(entity);

        //  修改应用状态和应用默认的规则，
        //  如果修改默认值为true，那么需要把状态设置为可用
        //  如果修改默认值为false，那么需要把状态改为不可用
        //  如果当前应用已被禁用，那么修改默认值不可更新应用状态
        if (application.getStatus() == CommonStatusEnum.LOCK) {
            return;
        }

        // 更新应用
        tenantOps.updateApplication(application,
                BooleanUtil.isTrue(params.getDefaultApp()) ? CommonStatusEnum.ENABLE : CommonStatusEnum.LOCK);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeApplication(long id) {
        SysApplication application = sysApplicationService.getById(id);
        sysApplicationService.removeById(id);
        tenantOps.removeApplication(application);
    }

    @Override
    public void updateStatusOfTargetOrg(long orgId, SysApplicationTenant params) {
        TenantEnv.runAs(orgId, () -> {
            SysApplicationTenant entity = new SysApplicationTenant();
            entity.setId(params.getId());
            entity.setStatus(params.getStatus());
            sysApplicationTenantService.updateById(entity);
        });
    }
}
