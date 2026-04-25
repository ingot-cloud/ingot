package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.PlatformRole;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.PlatformRoleMapper;
import com.ingot.cloud.pms.service.domain.PlatformRoleService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.RoleUtil;
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
public class PlatformRoleServiceImpl extends BaseServiceImpl<PlatformRoleMapper, PlatformRole> implements PlatformRoleService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.PLATFORM_ROLES, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<PlatformRole> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.PLATFORM_ROLES, key = CacheKey.ItemKey, unless = "#result == null")
    public PlatformRole getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(value = CacheConstants.PLATFORM_ROLES, key = CacheKey.CodeKey, unless = "#result == null")
    public PlatformRole getByCode(String code) {
        return getOne(Wrappers.<PlatformRole>lambdaQuery()
                .eq(PlatformRole::getCode, code));
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_ROLES, allEntries = true)
    public void create(PlatformRole role) {
        createAndReturnResult(role);
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_ROLES, allEntries = true)
    public PlatformRole createAndReturnResult(PlatformRole role) {
        assertionChecker.checkOperation(role.getType() != null,
                "PlatformRoleServiceImpl.TypeNonNull");
        assertionChecker.checkOperation(role.getOrgType() != null,
                "PlatformRoleServiceImpl.OrgTypeNonNull");
        assertionChecker.checkOperation(
                role.getType() == RoleTypeEnum.ROLE,
                StrUtil.isNotEmpty(role.getCode()),
                "PlatformRoleServiceImpl.CodeNonNull"
        );
        assertionChecker.checkOperation(
                role.getType() == RoleTypeEnum.ROLE,
                count(Wrappers.<PlatformRole>lambdaQuery()
                        .eq(PlatformRole::getCode, role.getCode())) == 0,
                "PlatformRoleServiceImpl.ExistCode"
        );
        assertionChecker.checkOperation(
                role.getPid() != null,
                count(Wrappers.<PlatformRole>lambdaQuery()
                        .eq(PlatformRole::getId, role.getPid())) > 0,
                "PlatformRoleServiceImpl.ParentNonExist"
        );

        if (role.getStatus() == null) {
            role.setStatus(CommonStatusEnum.ENABLE);
        }

        role.setCreatedAt(DateUtil.now());
        role.setUpdatedAt(role.getCreatedAt());

        save(role);
        return role;
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_ROLES, allEntries = true)
    public void update(PlatformRole role) {
        // 角色编码不可修改
        role.setCode(null);
        // 角色类型不能修改
        role.setType(null);
        // 组织类型不可修改
        role.setOrgType(null);
        role.setUpdatedAt(DateUtil.now());
        updateById(role);
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_ROLES, allEntries = true)
    public void delete(long id) {
        PlatformRole role = SpringContextHolder.getBean(PlatformRoleService.class).getById(id);
        if (role == null) {
            return;
        }
        assertionChecker.checkOperation(!RoleUtil.isAdmin(role.getCode()),
                "PlatformRoleServiceImpl.SuperAdminRemoveFailed");

        // 叶子才可以删除
        boolean result = count(Wrappers.<PlatformRole>lambdaQuery()
                .eq(PlatformRole::getPid, id)) == 0;
        assertionChecker.checkOperation(result,
                "PlatformRoleServiceImpl.RemoveFailedMustLeaf");

        removeById(id);
    }
}
