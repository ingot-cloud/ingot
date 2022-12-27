package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.SysMenuMapper;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    private final AssertionChecker assertI18nService;
    private final MenuTrans menuTrans;

    @Override
    public List<MenuTreeNodeVO> getMenuByAuthorities(List<SysAuthority> authorities) {
        List<MenuTreeNodeVO> nodeList = SpringContextHolder.getBean(SysMenuService.class)
                .nodeList().stream()
                .filter(node -> node.getAuthorityId() == null ||
                        authorities.stream()
                                .anyMatch(authority -> node.getAuthorityId().equals(authority.getId())))
                .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        List<MenuTreeNodeVO> tree = TreeUtils.build(nodeList);
        TreeUtils.compensate(tree, nodeList);
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

        params.setCreatedAt(DateUtils.now());

        assertI18nService.checkOperation(save(params),
                "SysMenuServiceImpl.CreateFailed");
    }

    @Override
    @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    public void updateMenu(SysMenu params) {
        if (StrUtil.isNotEmpty(params.getPath())) {
            assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getPath, params.getPath())) == 0,
                    "SysMenuServiceImpl.ExistPath");
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
