package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.MetaRoleMapper;
import com.ingot.cloud.pms.service.domain.MetaRoleService;
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
public class MetaRoleServiceImpl extends BaseServiceImpl<MetaRoleMapper, MetaRole> implements MetaRoleService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.META_ROLES, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<MetaRole> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.META_ROLES, key = CacheKey.ItemKey, unless = "#result == null")
    public MetaRole getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(value = CacheConstants.META_ROLES, key = CacheKey.CodeKey, unless = "#result == null")
    public MetaRole getByCode(String code) {
        return getOne(Wrappers.<MetaRole>lambdaQuery()
                .eq(MetaRole::getCode, code));
    }

    @Override
    @CacheEvict(value = CacheConstants.META_ROLES, allEntries = true)
    public void create(MetaRole role) {
        createAndReturnResult(role);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_ROLES, allEntries = true)
    public MetaRole createAndReturnResult(MetaRole role) {
        assertionChecker.checkOperation(role.getType() != null,
                "MetaRoleServiceImpl.TypeNonNull");
        assertionChecker.checkOperation(role.getOrgType() != null,
                "MetaRoleServiceImpl.OrgTypeNonNull");
        assertionChecker.checkOperation(
                role.getType() == RoleTypeEnum.ROLE,
                StrUtil.isNotEmpty(role.getCode()),
                "MetaRoleServiceImpl.CodeNonNull"
        );
        assertionChecker.checkOperation(
                role.getType() == RoleTypeEnum.ROLE,
                count(Wrappers.<MetaRole>lambdaQuery()
                        .eq(MetaRole::getCode, role.getCode())) == 0,
                "MetaRoleServiceImpl.ExistCode"
        );
        assertionChecker.checkOperation(
                role.getPid() != null,
                count(Wrappers.<MetaRole>lambdaQuery()
                        .eq(MetaRole::getId, role.getPid())) > 0,
                "MetaRoleServiceImpl.ParentNonExist"
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
    @CacheEvict(value = CacheConstants.META_ROLES, allEntries = true)
    public void update(MetaRole role) {
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
    @CacheEvict(value = CacheConstants.META_ROLES, allEntries = true)
    public void delete(long id) {
        MetaRole role = SpringContextHolder.getBean(MetaRoleService.class).getById(id);
        if (role == null) {
            return;
        }
        assertionChecker.checkOperation(!RoleUtil.isAdmin(role.getCode()),
                "MetaRoleServiceImpl.SuperAdminRemoveFailed");

        // 叶子才可以删除
        boolean result = count(Wrappers.<MetaRole>lambdaQuery()
                .eq(MetaRole::getPid, id)) == 0;
        assertionChecker.checkOperation(result,
                "MetaRoleServiceImpl.RemoveFailedMustLeaf");

        removeById(id);
    }
}
