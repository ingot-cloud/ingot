package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.SysAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
public class SysAuthorityServiceImpl extends BaseServiceImpl<SysAuthorityMapper, SysAuthority> implements SysAuthorityService {
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysMenuService sysMenuService;
    private final AssertionChecker assertI18nService;
    private final AuthorityTrans authorityTrans;

    @Override
    @Cacheable(value = CacheConstants.AUTHORITY_DETAILS, key = CacheKey.AuthorityListKey, unless = "#result.isEmpty()")
    public List<SysAuthority> list() {
        return super.list();
    }

    @Override
    public List<SysAuthority> getAuthorityByRoles(List<SysRole> roles) {
        return roles.stream()
                .flatMap(role -> sysRoleAuthorityService.getAuthoritiesByRole(role.getId()).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<SysAuthority> getAuthorityAndChildrenByRoles(List<SysRole> roles) {
        List<SysAuthority> all = SpringContextHolder.getBean(SysAuthorityService.class).list();
        CopyOnWriteArrayList<SysAuthority> authorities = new CopyOnWriteArrayList<>(getAuthorityByRoles(roles));
        authorities.forEach(item -> fillChildren(authorities, all, item));
        return authorities;
    }

    /**
     * 填充子权限
     */
    private void fillChildren(List<SysAuthority> result, List<SysAuthority> all, SysAuthority parent) {
        all.stream()
                .filter(item -> item.getPid() != null && item.getPid().equals(parent.getId()))
                .forEach(item -> {
                    if (result.stream().noneMatch(a -> Objects.equals(a.getId(), item.getId()))) {
                        result.add(item);
                    }
                    fillChildren(result, all, item);
                });
    }

    @Override
    public List<AuthorityTreeNodeVO> treeList() {
        // 同类调用方法不触发aop，导致@Cacheable无法工作，从而缓存失败
        // 所以使用
        List<AuthorityTreeNodeVO> nodeList = SpringContextHolder
                .getBean(SysAuthorityService.class)
                .list()
                .stream()
                .sorted(Comparator.comparing(SysAuthority::getId))
                .map(authorityTrans::to).collect(Collectors.toList());
        return TreeUtils.build(nodeList);
    }

    @Override
    public List<AuthorityTreeNodeVO> treeList(SysAuthority condition) {
        List<AuthorityTreeNodeVO> nodeList = SpringContextHolder
                .getBean(SysAuthorityService.class)
                .list()
                .stream()
                .filter(BizFilter.authorityFilter(condition))
                .sorted(Comparator.comparing(SysAuthority::getId))
                .map(authorityTrans::to).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtils.build(nodeList);
        TreeUtils.compensate(tree, nodeList);
        return tree;
    }

    @Override
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, allEntries = true)
    public void createAuthority(SysAuthority params) {
        // code 不能重复
        assertI18nService.checkOperation(count(Wrappers.<SysAuthority>lambdaQuery()
                        .eq(SysAuthority::getCode, params.getCode())) == 0,
                "SysAuthorityServiceImpl.ExistCode");

        // 检测父权限，填充编码
        params.setCode(formatCode(params.getPid(), params.getCode()));
        params.setCreatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysAuthorityServiceImpl.CreateFailed");
    }

    private String formatCode(Long pid, String code) {
        return Optional.ofNullable(pid)
                .filter(id -> id != IDConstants.ROOT_TREE_ID)
                .map(this::getById)
                .map(item -> item.getCode() + StrUtil.DOT + code)
                .orElse(code);
    }

    @Override
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, allEntries = true)
    public void updateAuthority(SysAuthority params) {
        // 权限编码不可更新
        params.setCode(null);
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysAuthorityServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, allEntries = true),
            @CacheEvict(value = CacheConstants.MENU_DETAILS, allEntries = true)
    })
    public void removeAuthorityById(long id) {
        // 叶子权限才可以删除
        boolean result = count(Wrappers.<SysAuthority>lambdaQuery().eq(SysAuthority::getPid, id)) == 0;
        assertI18nService.checkOperation(result, "SysAuthorityServiceImpl.RemoveFailedMustLeaf");

        // 取消关联的角色
        sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(SysRoleAuthority::getAuthorityId, id));

        // 取消关联菜单
        sysMenuService.nodeList().forEach(menu -> {
            if (ObjectUtil.equals(menu.getAuthorityId(), id)) {
                SysMenu sysMenu = new SysMenu();
                sysMenu.setId(menu.getId());
                sysMenu.setAuthorityId(0L);
                sysMenu.updateById();
            }
        });

        result = removeById(id);
        assertI18nService.checkOperation(result, "SysAuthorityServiceImpl.RemoveFailed");
    }
}
