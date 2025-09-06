package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.core.MenuUtils;
import com.ingot.cloud.pms.service.biz.BizMenuService;
import com.ingot.cloud.pms.service.domain.SysApplicationService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizMenuServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/5/19.</p>
 * <p>Time         : 08:53.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizMenuServiceImpl implements BizMenuService {
    private final SysMenuService sysMenuService;
    private final SysAuthorityService sysAuthorityService;
    private final SysApplicationService sysApplicationService;

    private final AssertionChecker assertI18nService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void createMenu(SysMenu params) {
        if (params.getLinkType() == null) {
            params.setLinkType(MenuLinkTypeEnum.Default);
        }
        // 链接类型不是默认, 自动生成path
        if (params.getLinkType() != MenuLinkTypeEnum.Default) {
            MenuUtils.setMenuOuterLinkPath(params, params.getPid(), sysMenuService);
        }

        // 外部链接，path可以为空
        assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getPath()), "SysMenu.path");
        assertI18nService.checkOperation(sysMenuService.count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPath, params.getPath())) == 0,
                "SysMenuServiceImpl.ExistPath");

        SysMenu parent = null;
        // 如果是子菜单， 默认和父菜单orgType一致
        if (params.getPid() != null && params.getPid() > 0) {
            parent = sysMenuService.getById(params.getPid());
            assertI18nService.checkOperation(parent != null, "SysMenuServiceImpl.ParentNonExist");
            assert parent != null;
            params.setOrgType(parent.getOrgType());
        }

        // 如果是自定义视图路径，则viewPath不能为空
        if (BooleanUtil.isTrue(params.getCustomViewPath())) {
            assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                    "SysMenuServiceImpl.ViewPathNotNull");
        } else {
            MenuUtils.setViewPathAccordingToPath(params);
        }

        params.setCreatedAt(DateUtil.now());

        // 同步创建权限，并且关联
        Long authorityId = createAuthorityId(params, parent);
        params.setAuthorityId(authorityId);

        assertI18nService.checkOperation(sysMenuService.save(params),
                "SysMenuServiceImpl.CreateFailed");
    }

    private Long createAuthorityId(SysMenu params, SysMenu parent) {
        // 如果存在pid，那么查询parent，获取parent的authority
        Long pid = parent != null ? parent.getAuthorityId() : null;

        SysAuthority authority = new SysAuthority();
        authority.setPid(pid);
        authority.setName(params.getName());
        authority.setCode(MenuUtils.getMenuAuthorityCode(params));
        authority.setStatus(params.getStatus());
        authority.setType(params.getOrgType());

        sysAuthorityService.createAuthority(authority, false);
        return authority.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void updateMenu(SysMenu params) {
        SysMenu current = sysMenuService.getById(params.getId());
        assertI18nService.checkOperation(current != null,
                "SysMenuServiceImpl.NonExist");
        assert current != null;

        // 菜单权限编码不可修改，菜单pid不可修改
        params.setPid(null);
        params.setAuthorityId(null);

        // 路径不为空，需要判断是否重复
        if (StrUtil.isNotEmpty(params.getPath())) {
            assertI18nService.checkOperation(sysMenuService.count(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getPath, params.getPath())) == 0,
                    "SysMenuServiceImpl.ExistPath");
        }

        // 如果修改了链接类型，并且修改的内容不是默认类型，那么需要自动处理path
        if (params.getLinkType() != null && params.getLinkType() != MenuLinkTypeEnum.Default) {
            MenuUtils.setMenuOuterLinkPath(params, current.getPid(), sysMenuService);
            // 修改为外部链接，customViewPath设置为false
            params.setCustomViewPath(Boolean.FALSE);
        }

        if (params.getCustomViewPath() == null) {
            params.setCustomViewPath(current.getCustomViewPath());
        }
        if (params.getProps() == null) {
            params.setProps(current.getProps());
        }

        // 非自定义视图
        if (BooleanUtil.isFalse(params.getCustomViewPath())) {
            // path不为空，并且链接类型是默认类型
            if (StrUtil.isNotEmpty(params.getPath())
                    && (params.getLinkType() == MenuLinkTypeEnum.Default
                    || (params.getLinkType() == null && current.getLinkType() == MenuLinkTypeEnum.Default))) {
                // 如果修改了路径，那么需要修改默认视图path
                MenuUtils.setViewPathAccordingToPath(params);
            }
        } else {
            // 自定义视图
            // 如果当前自定义视图路径是空，那么更新字段不能为空
            if (StrUtil.isEmpty(current.getViewPath())) {
                assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                        "SysMenuServiceImpl.ViewPathNotNull");
            }
        }

        params.setUpdatedAt(DateUtil.now());
        assertI18nService.checkOperation(sysMenuService.updateById(params),
                "SysMenuServiceImpl.UpdateFailed");

        // 更新权限
        SysAuthority authority = new SysAuthority();
        authority.setId(current.getAuthorityId());
        authority.setName(params.getName());
        authority.setStatus(params.getStatus());
        authority.setType(params.getOrgType());
        sysAuthorityService.updateAuthority(authority);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void removeMenuById(long id) {
        SysMenu current = sysMenuService.getById(id);
        assertI18nService.checkOperation(current != null,
                "SysMenuServiceImpl.NonExist");
        assert current != null;

        // 判断是否为叶子节点
        assertI18nService.checkOperation(sysMenuService.count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPid, id)) == 0,
                "SysMenuServiceImpl.ExistLeaf");

        // 判断是否为应用，如果是应用那么不可删除
        assertI18nService.checkOperation(sysApplicationService.count(Wrappers.<SysApplication>lambdaQuery()
                        .eq(SysApplication::getMenuId, id)) == 0,
                "SysMenuServiceImpl.IsApplication");

        assertI18nService.checkOperation(sysMenuService.removeById(id), "SysMenuServiceImpl.RemoveFailed");

        sysAuthorityService.removeAuthorityById(current.getAuthorityId());
    }
}
