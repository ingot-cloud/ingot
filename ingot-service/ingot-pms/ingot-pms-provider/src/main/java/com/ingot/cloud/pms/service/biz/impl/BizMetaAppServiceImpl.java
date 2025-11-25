package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.service.biz.BizMetaAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.MetaAppService;
import com.ingot.cloud.pms.service.domain.MetaMenuService;
import com.ingot.cloud.pms.service.domain.TenantAppConfigService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizMetaAppServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 16:43.</p>
 */
@Service
@RequiredArgsConstructor
public class BizMetaAppServiceImpl implements BizMetaAppService {
    private final MetaAppService appService;
    private final MetaMenuService menuService;
    private final TenantAppConfigService tenantAppConfigService;

    private final BizRoleService bizRoleService;

    private final AssertionChecker assertionChecker;

    @Override
    public IPage<MetaApp> conditionPage(Page<MetaApp> page, MetaApp condition) {
        return appService.conditionPage(page, condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(MetaApp params) {
        assertionChecker.checkOperation(params.getMenuId() != null,
                "BizMetaAppServiceImpl.MenuNonNull");
        MetaMenu menu = menuService.getById(params.getMenuId());
        assertionChecker.checkOperation(menu != null,
                "BizMetaAppServiceImpl.MenuNonNull");
        assert menu != null;

        params.setAuthorityId(menu.getAuthorityId());
        if (StrUtil.isEmpty(params.getName())) {
            params.setName(menu.getName());
        }
        if (StrUtil.isEmpty(params.getIcon())) {
            params.setIcon(menu.getIcon());
        }

        appService.create(params);

        // 创建一个应用，就需要给组织管理员角色绑定相关权限
        bizRoleService.orgManagerAssignAuthorities(List.of(params.getAuthorityId()), true);
    }

    @Override
    public void update(MetaApp params) {
        if (params.getMenuId() != null) {
            assertionChecker.checkOperation(
                    menuService.count(Wrappers.<MetaMenu>lambdaQuery()
                            .eq(MetaMenu::getId, params.getId())) > 0,
                    "BizMetaAppServiceImpl.MenuNonNull");
        }

        appService.update(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        MetaApp app = appService.getById(id);
        assertionChecker.checkOperation(app != null, "BizMetaAppServiceImpl.NonNull");
        assert app != null;

        appService.delete(id);
        tenantAppConfigService.clearByAppId(id);
        // 删除一个应用，要给组织管理员取消相关权限
        bizRoleService.orgManagerAssignAuthorities(List.of(app.getAuthorityId()), false);
    }
}
