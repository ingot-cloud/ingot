package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.core.BizMenuUtils;
import com.ingot.cloud.pms.service.biz.BizPlatformAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.PlatformMenuService;
import com.ingot.cloud.pms.service.domain.TenantAppConfigService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizPlatformAppServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 16:43.</p>
 */
@Service
@RequiredArgsConstructor
public class BizPlatformAppServiceImpl implements BizPlatformAppService {
    private final PlatformAppService appService;
    private final PlatformMenuService menuService;
    private final TenantAppConfigService tenantAppConfigService;

    private final BizRoleService bizRoleService;

    private final AssertionChecker assertionChecker;

    @Override
    public IPage<PlatformApp> conditionPage(Page<PlatformApp> page, PlatformApp condition) {
        return appService.conditionPage(page, condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(PlatformApp params) {
        assertionChecker.checkOperation(params.getMenuId() != null,
                "BizPlatformAppServiceImpl.MenuNonNull");
        PlatformMenu menu = menuService.getById(params.getMenuId());
        assertionChecker.checkOperation(menu != null,
                "BizPlatformAppServiceImpl.MenuNonNull");
        assert menu != null;

        params.setPermissionId(menu.getPermissionId());
        if (params.getAppType() == null) {
            params.setAppType(OrgTypeEnum.Tenant);
        }
        if (StrUtil.isEmpty(params.getCode())) {
            params.setCode(BizMenuUtils.getMenuAuthorityCode(menu));
        }
        if (params.getSort() == null) {
            params.setSort(menu.getSort());
        }
        if (StrUtil.isEmpty(params.getName())) {
            params.setName(menu.getName());
        }
        if (StrUtil.isEmpty(params.getIcon())) {
            params.setIcon(menu.getIcon());
        }

        appService.create(params);

        // 创建一个应用，就需要给组织管理员角色绑定相关权限
        bizRoleService.orgManagerAssignPermissions(List.of(params.getPermissionId()), true);
    }

    @Override
    public void update(PlatformApp params) {
        if (params.getMenuId() != null) {
            assertionChecker.checkOperation(
                    menuService.count(Wrappers.<PlatformMenu>lambdaQuery()
                            .eq(PlatformMenu::getId, params.getId())) > 0,
                    "BizPlatformAppServiceImpl.MenuNonNull");
        }

        appService.update(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        PlatformApp app = appService.getById(id);
        assertionChecker.checkOperation(app != null, "BizPlatformAppServiceImpl.NonNull");
        assert app != null;

        appService.delete(id);
        tenantAppConfigService.clearByAppId(id);
        // 删除一个应用，要给组织管理员取消相关权限
        bizRoleService.orgManagerAssignPermissions(List.of(app.getPermissionId()), false);
    }
}
