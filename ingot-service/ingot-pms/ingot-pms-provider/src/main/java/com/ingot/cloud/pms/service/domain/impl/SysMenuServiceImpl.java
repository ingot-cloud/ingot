package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.dto.menu.MenuFilterDTO;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnums;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.core.MenuUtils;
import com.ingot.cloud.pms.mapper.SysMenuMapper;
import com.ingot.cloud.pms.service.domain.SysApplicationService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.UUIDUtils;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    private final AssertionChecker assertI18nService;
    private final SysApplicationService sysApplicationService;

    @Override
    public List<MenuTreeNodeVO> getMenuByAuthorities(List<SysAuthority> authorities) {
        List<MenuTreeNodeVO> allNodeList = SpringContextHolder
                .getBean(SysMenuService.class).nodeList();
        List<MenuTreeNodeVO> nodeList = MenuUtils.filterMenus(allNodeList, authorities);
        List<MenuTreeNodeVO> tree = TreeUtils.build(nodeList);
        log.debug("[SysMenuServiceImpl] - tree={}", tree);
        return tree;
    }

    @Override
    @Cacheable(value = CacheConstants.MENU_DETAILS + "#" + CacheKey.DefaultExpiredTimeSeconds,
            key = CacheKey.MenuListKey,
            unless = "#result.isEmpty()")
    public List<MenuTreeNodeVO> nodeList() {
        return CollUtil.emptyIfNull(baseMapper.getAll());
    }

    @Override
    public List<MenuTreeNodeVO> treeList(MenuFilterDTO filter) {
        List<MenuTreeNodeVO> allNode = SpringContextHolder.getBean(SysMenuService.class)
                .nodeList().stream()
                .filter(BizFilter.menuFilter(filter))
                .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        return TreeUtils.build(allNode);
    }

    @Override
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void createMenu(SysMenu params) {
        // 外部链接，path可以为空
        assertI18nService.checkOperation(params.getLinkType() == MenuLinkTypeEnums.IFrame
                || StrUtil.isNotEmpty(params.getPath()), "SysMenu.path");
        assertI18nService.checkOperation(params.getLinkType() == MenuLinkTypeEnums.IFrame
                        || count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPath, params.getPath())) == 0,
                "SysMenuServiceImpl.ExistPath");

        // 如果是自定义视图路径，则viewPath不能为空
        if (BooleanUtil.isTrue(params.getCustomViewPath())) {
            assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                    "SysMenuServiceImpl.ViewPathNotNull");
        } else {
            setViewPathAccordingToPath(params);
        }

        if (params.getLinkType() == null) {
            params.setLinkType(MenuLinkTypeEnums.Default);
        }

        // 链接类型不是默认, 自动处理菜单路由
        if (params.getLinkType() != MenuLinkTypeEnums.Default) {
            setMenuOuterLinkPath(params, params.getPid());
        }

        params.setCreatedAt(DateUtils.now());

        assertI18nService.checkOperation(save(params),
                "SysMenuServiceImpl.CreateFailed");
    }

    @Override
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void updateMenu(SysMenu params) {
        SysMenu current = getById(params.getId());
        assertI18nService.checkOperation(current != null,
                "SysMenuServiceImpl.NonExist");
        assert current != null;

        // 路径不为空，需要判断是否重复
        if (StrUtil.isNotEmpty(params.getPath())) {
            assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getPath, params.getPath())) == 0,
                    "SysMenuServiceImpl.ExistPath");
        }

        // 如果修改了链接类型，并且修改的内容不是默认类型，那么需要自动处理path
        if (params.getLinkType() != null && params.getLinkType() != MenuLinkTypeEnums.Default) {
            setMenuOuterLinkPath(params, current.getPid());
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
                    && (params.getLinkType() == MenuLinkTypeEnums.Default
                    || (params.getLinkType() == null && current.getLinkType() == MenuLinkTypeEnums.Default))) {
                // 如果修改了路径，那么需要修改默认视图path
                setViewPathAccordingToPath(params);
            }
        } else {
            // 自定义视图
            // 如果当前自定义视图路径是空，那么更新字段不能为空
            if (StrUtil.isEmpty(current.getViewPath())) {
                assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                        "SysMenuServiceImpl.ViewPathNotNull");
            }
        }

        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysMenuServiceImpl.UpdateFailed");
    }

    private void setMenuOuterLinkPath(SysMenu params, Long pid) {
        String pPath = "";
        if (pid != null && pid > 0) {
            SysMenu parent = getById(pid);
            pPath = parent.getPath() + "/";
        } else {
            pPath = "/";
        }
        String path = pPath + UUIDUtils.generateShortUuid();
        params.setPath(path);
    }

    private void setViewPathAccordingToPath(SysMenu menu) {
        String path = menu.getPath();
        if (BooleanUtil.isTrue(menu.getProps())) {
            path = StrUtil.subBefore(path, "/", true);
        }
        menu.setViewPath("@/pages" + path + "/IndexPage.vue");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void removeMenuById(long id) {
        // 判断是否为叶子节点
        assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPid, id)) == 0,
                "SysMenuServiceImpl.ExistLeaf");

        // 判断是否为应用，如果是应用那么不可删除
        assertI18nService.checkOperation(sysApplicationService.count(Wrappers.<SysApplication>lambdaQuery()
                        .eq(SysApplication::getMenuId, id)) == 0,
                "SysMenuServiceImpl.IsApplication");

        assertI18nService.checkOperation(removeById(id), "SysMenuServiceImpl.RemoveFailed");
    }
}
