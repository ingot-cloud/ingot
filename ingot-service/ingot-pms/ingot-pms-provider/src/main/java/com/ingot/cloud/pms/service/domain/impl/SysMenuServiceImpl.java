package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.dto.menu.MenuFilterDTO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.core.MenuUtils;
import com.ingot.cloud.pms.mapper.SysMenuMapper;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

    @Override
    public List<MenuTreeNodeVO> getMenuByAuthorities(List<SysAuthority> authorities) {
        List<MenuTreeNodeVO> allNodeList = SpringContextHolder
                .getBean(SysMenuService.class).nodeList();
        List<MenuTreeNodeVO> nodeList = MenuUtils.filterMenus(allNodeList, authorities);
        List<MenuTreeNodeVO> tree = TreeUtils.build(nodeList)
                .stream()
                .sorted(Comparator.comparing(MenuTreeNodeVO::getSort))
                .toList();
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
                .sorted(Comparator.comparing(MenuTreeNodeVO::getOrgType)
                        .thenComparing(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        return TreeUtils.build(allNode);
    }
}
