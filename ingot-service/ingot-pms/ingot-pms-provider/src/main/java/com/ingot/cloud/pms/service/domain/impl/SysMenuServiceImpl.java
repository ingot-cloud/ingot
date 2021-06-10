package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNode;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.mapper.SysMenuMapper;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.cloud.pms.service.domain.SysRoleMenuService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
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
@Service
@AllArgsConstructor
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    private final SysRoleMenuService sysRoleMenuService;

    private final IdGenerator idGenerator;
    private final AssertI18nService assertI18nService;
    private final MenuTrans menuTrans;

    @Override
    public List<MenuTreeNode> tree() {
        List<SysMenu> all = list();

        List<MenuTreeNode> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysMenu::getSort))
                .map(menuTrans::to).collect(Collectors.toList());

        return TreeUtils.build(allNode, 0);
    }

    @Override
    public void createMenu(SysMenu params) {
        assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPath, params.getPath())) == 0,
                "SysMenuServiceImpl.ExistPath");

        params.setId(idGenerator.nextId());
        params.setCreatedAt(DateUtils.now());

        assertI18nService.checkOperation(save(params),
                "SysMenuServiceImpl.CreateFailed");
    }

    @Override
    public void updateMenu(SysMenu params) {
        if (StrUtil.isNotEmpty(params.getPath())) {
            assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                            .eq(SysMenu::getPath, params.getPath())) == 0,
                    "SysMenuServiceImpl.ExistPath");
        }

        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysMenuServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMenuById(long id) {
        // 判断是否为叶子节点
        assertI18nService.checkOperation(count(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPid, id)) == 0,
                "SysMenuServiceImpl.ExistLeaf");

        // 取消角色关联
        sysRoleMenuService.remove(Wrappers.<SysRoleMenu>lambdaQuery()
                .eq(SysRoleMenu::getMenuId, id));

        assertI18nService.checkOperation(removeById(id), "SysMenuServiceImpl.RemoveFailed");
    }
}
