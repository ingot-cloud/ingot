package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.core.MenuUtils;
import com.ingot.cloud.pms.mapper.MetaMenuMapper;
import com.ingot.cloud.pms.service.domain.MetaMenuService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
@RequiredArgsConstructor
public class MetaMenuServiceImpl extends BaseServiceImpl<MetaMenuMapper, MetaMenu> implements MetaMenuService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.META_MENUS + "#" + CacheKey.DefaultExpiredTimeSeconds,
            key = CacheKey.ListKey,
            unless = "#result.isEmpty()")
    public List<MenuTreeNodeVO> nodeList() {
        return CollUtil.emptyIfNull(baseMapper.getAll());
    }

    @Override
    @Cacheable(value = CacheConstants.META_MENUS + "#" + CacheKey.DefaultExpiredTimeSeconds,
            key = CacheKey.ItemKey,
            unless = "#result == null")
    public MetaMenu getById(Serializable id) {
        MetaMenu menu = super.getById(id);
        assertionChecker.checkOperation(menu != null,
                "MetaMenuServiceImpl.NonExist");
        return menu;
    }

    @Override
    @CacheEvict(value = CacheConstants.META_MENUS, allEntries = true)
    public void create(MetaMenu params) {
        if (params.getLinkType() == null) {
            params.setLinkType(MenuLinkTypeEnum.Default);
        }
        // 链接类型不是默认, 自动生成path
        if (params.getLinkType() != MenuLinkTypeEnum.Default) {
            MenuUtils.setMenuOuterLinkPath(params, params.getPid(), this);
        }

        // 外部链接，path可以为空
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPath()), "MetaMenuServiceImpl.PathNonNull");
        assertionChecker.checkOperation(count(Wrappers.<MetaMenu>lambdaQuery()
                        .eq(MetaMenu::getPath, params.getPath())) == 0,
                "MetaMenuServiceImpl.ExistPath");

        // 如果是子菜单， 默认和父菜单orgType一致
        if (params.getPid() != null && params.getPid() > 0) {
            MetaMenu parent = innerGetById(params.getPid());
            assertionChecker.checkOperation(parent != null, "MetaMenuServiceImpl.ParentNonExist");
            assert parent != null;
            params.setOrgType(parent.getOrgType());
        }

        // 如果是自定义视图路径，则viewPath不能为空
        if (BooleanUtil.isTrue(params.getCustomViewPath())) {
            assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                    "MetaMenuServiceImpl.ViewPathNotNull");
        } else {
            MenuUtils.setViewPathAccordingToPath(params);
        }

        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(params.getCreatedAt());

        save(params);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_MENUS, allEntries = true)
    public void update(MetaMenu params) {
        MetaMenu current = innerGetById(params.getId());
        assertionChecker.checkOperation(current != null,
                "MetaMenuServiceImpl.NonExist");
        assert current != null;

        // 菜单权限编码不可修改，菜单pid不可修改
        params.setPid(null);
        params.setAuthorityId(null);

        // 路径不为空，需要判断是否重复
        if (StrUtil.isNotEmpty(params.getPath())) {
            assertionChecker.checkOperation(count(Wrappers.<MetaMenu>lambdaQuery()
                            .eq(MetaMenu::getPath, params.getPath())) == 0,
                    "MetaMenuServiceImpl.ExistPath");
        }

        // 如果修改了链接类型，并且修改的内容不是默认类型，那么需要自动处理path
        if (params.getLinkType() != null && params.getLinkType() != MenuLinkTypeEnum.Default) {
            MenuUtils.setMenuOuterLinkPath(params, current.getPid(), this);
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
                assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getViewPath()),
                        "MetaMenuServiceImpl.ViewPathNotNull");
            }
        }

        params.setUpdatedAt(DateUtil.now());
        updateById(params);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_MENUS, allEntries = true)
    public void delete(long id) {
        MetaMenu current = innerGetById(id);
        assertionChecker.checkOperation(current != null,
                "MetaMenuServiceImpl.NonExist");
        assert current != null;

        // 判断是否为叶子节点
        assertionChecker.checkOperation(count(Wrappers.<MetaMenu>lambdaQuery()
                        .eq(MetaMenu::getPid, id)) == 0,
                "MetaMenuServiceImpl.ExistLeaf");

        removeById(id);
    }

    private MetaMenu innerGetById(Long id) {
        return SpringContextHolder.getBean(MetaMenuService.class).getById(id);
    }
}
