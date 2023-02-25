package com.ingot.cloud.pms.service.domain.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.SysMenuMapper;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<MenuTreeNodeVO> getMenuByAuthorities(List<SysAuthority> authorities) {
        List<MenuTreeNodeVO> allNodeList = SpringContextHolder
                .getBean(SysMenuService.class).nodeList();
        List<MenuTreeNodeVO> nodeList = allNodeList.stream()
                .filter(node -> node.getAuthorityId() == null || node.getAuthorityId() == 0 ||
                        authorities.stream()
                                .anyMatch(authority -> node.getAuthorityId().equals(authority.getId())))
                .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        // 如果过滤后的列表中存在父节点，并且不在当前列表中，那么需要增加
        List<MenuTreeNodeVO> copy = new ArrayList<>(nodeList);
        copy.stream()
                .filter(node -> node.getPid() != IDConstants.ROOT_TREE_ID)
                .forEach(node -> {
                    if (nodeList.stream().noneMatch(item -> ObjectUtil.equals(item.getId(), node.getPid()))) {
                        allNodeList.stream()
                                .filter(item -> ObjectUtil.equals(item.getId(), node.getPid()))
                                .findFirst()
                                .ifPresent(nodeList::add);
                    }
                });

        List<MenuTreeNodeVO> tree = TreeUtils.build(nodeList);
        log.debug("[SysMenuServiceImpl] - tree={}", tree);
        return tree;
    }

    @Override
    @Cacheable(value = CacheConstants.MENU_DETAILS, key = CacheKey.MenuListKey, unless = "#result.isEmpty()")
    public List<MenuTreeNodeVO> nodeList() {
        return CollUtil.emptyIfNull(baseMapper.getAll());
    }

    @Override
    public List<MenuTreeNodeVO> treeList() {
        List<MenuTreeNodeVO> allNode = SpringContextHolder.getBean(SysMenuService.class)
                .nodeList().stream()
                .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        return TreeUtils.build(allNode);
    }

    @Override
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void createMenu(SysMenu params) {
        assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPath, params.getPath())) == 0,
                "SysMenuServiceImpl.ExistPath");

        if (BooleanUtil.isTrue(params.getCustomViewPath())) {
            assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                    "SysMenuServiceImpl.ViewPathNotNull");
        } else {
            String path = params.getPath();
            params.setViewPath("@/pages" + path + "/IndexPage.vue");
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

        // 路径不为空，需要判断是否重复
        if (StrUtil.isNotEmpty(params.getPath())) {
            assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getPath, params.getPath())) == 0,
                    "SysMenuServiceImpl.ExistPath");
        }

        if (params.getCustomViewPath() == null) {
            params.setCustomViewPath(current.getCustomViewPath());
        }

        // 非自定义视图
        if (BooleanUtil.isFalse(params.getCustomViewPath())) {
            if (StrUtil.isNotEmpty(params.getPath())) {
                // 如果修改了路径，那么需要修改默认视图path
                String path = params.getPath();
                params.setViewPath("@/pages" + path + "/IndexPage.vue");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void removeMenuById(long id) {
        // 判断是否为叶子节点
        assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPid, id)) == 0,
                "SysMenuServiceImpl.ExistLeaf");

        assertI18nService.checkOperation(removeById(id), "SysMenuServiceImpl.RemoveFailed");
    }
}
