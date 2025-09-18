package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.SysAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysApplicationService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantEnv;
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
    private final SysApplicationService sysApplicationService;
    private final AssertionChecker assertI18nService;
    private final AuthorityConvert authorityConvert;

    @Override
    @Cacheable(value = CacheConstants.AUTHORITY_DETAILS, key = CacheKey.AuthorityListKey, unless = "#result.isEmpty()")
    public List<SysAuthority> list() {
        return super.list();
    }

    @Override
    public List<SysAuthority> getAuthorityByRoles(List<SysRole> roles) {
        return roles.stream()
                .flatMap(role -> TenantEnv.applyAs(role.getTenantId(),
                        () -> sysRoleAuthorityService.getAuthoritiesByRole(role.getId()).stream()))
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
                .map(authorityConvert::to).collect(Collectors.toList());
        return TreeUtil.build(nodeList);
    }

    @Override
    public List<AuthorityTreeNodeVO> treeList(AuthorityFilterDTO filter) {
        List<AuthorityTreeNodeVO> nodeList = SpringContextHolder
                .getBean(SysAuthorityService.class)
                .list()
                .stream()
                .filter(BizFilter.authorityFilter(filter))
                .sorted(Comparator.comparing(SysAuthority::getType)
                        .thenComparing(SysAuthority::getId))
                .map(authorityConvert::to).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    @Override
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, allEntries = true)
    public void createAuthority(SysAuthority params, boolean fillParentCode) {
        // code 不能重复
        assertI18nService.checkOperation(count(Wrappers.<SysAuthority>lambdaQuery()
                        .eq(SysAuthority::getCode, params.getCode())) == 0,
                "SysAuthorityServiceImpl.ExistCode");

        // 检测父权限，填充编码
        if (fillParentCode) {
            params.setCode(formatCode(params.getPid(), params.getCode()));
        }
        params.setCreatedAt(DateUtil.now());
        assertI18nService.checkOperation(save(params),
                "SysAuthorityServiceImpl.CreateFailed");
    }

    private String formatCode(Long pid, String code) {
        return Optional.ofNullable(pid)
                .filter(id -> id != IDConstants.ROOT_TREE_ID)
                .map(this::getById)
                .map(item -> item.getCode() + StrUtil.COLON + code)
                .orElse(code);
    }

    @Override
    @CacheEvict(value = CacheConstants.AUTHORITY_DETAILS, allEntries = true)
    public void updateAuthority(SysAuthority params) {
        // 权限编码不可更新
        params.setCode(null);
        params.setUpdatedAt(DateUtil.now());
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

        // 判断是否为应用，如果是应用那么不可删除
        assertI18nService.checkOperation(sysApplicationService.count(Wrappers.<SysApplication>lambdaQuery()
                        .eq(SysApplication::getAuthorityId, id)) == 0,
                "SysAuthorityServiceImpl.IsApplication");

        // 取消关联的角色
        sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(SysRoleAuthority::getAuthorityId, id));

        result = removeById(id);
        assertI18nService.checkOperation(result, "SysAuthorityServiceImpl.RemoveFailed");
    }
}
