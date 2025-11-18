package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.cloud.pms.mapper.TenantRolePrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantRolePrivateService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
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
public class TenantRolePrivateServiceImpl extends BaseServiceImpl<TenantRolePrivateMapper, TenantRolePrivate> implements TenantRolePrivateService {
    private final AssertionChecker assertionChecker;
    private final BizIdGen bizIdGen;

    @Override
    @Cacheable(value = CacheConstants.TENANT_ROLES, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<TenantRolePrivate> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.TENANT_ROLES, key = CacheKey.ItemKey, unless = "#result == null")
    public TenantRolePrivate getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(value = CacheConstants.TENANT_ROLES, key = CacheKey.CodeKey, unless = "#result == null")
    public TenantRolePrivate getByCode(String code) {
        return getOne(Wrappers.<TenantRolePrivate>lambdaQuery()
                .eq(TenantRolePrivate::getCode, code));
    }

    @Override
    @CacheEvict(value = CacheConstants.TENANT_ROLES, allEntries = true)
    public void create(TenantRolePrivate role) {
        createAndReturnResult(role);
    }

    @Override
    @CacheEvict(value = CacheConstants.TENANT_ROLES, allEntries = true)
    public TenantRolePrivate createAndReturnResult(TenantRolePrivate role) {
        assertionChecker.checkOperation(role.getType() != null,
                "TenantRolePrivateServiceImpl.TypeNonNull");
        assertionChecker.checkOperation(
                role.getType() == RoleTypeEnum.ROLE,
                StrUtil.isNotEmpty(role.getCode()),
                "TenantRolePrivateServiceImpl.CodeNonNull"
        );
        if (role.getType() == RoleTypeEnum.ROLE) {
            role.setCode(bizIdGen.genOrgRoleCode());
        }

        assertionChecker.checkOperation(
                role.getPid() != null,
                count(Wrappers.<TenantRolePrivate>lambdaQuery()
                        .eq(TenantRolePrivate::getId, role.getPid())) == 0,
                "TenantRolePrivateServiceImpl.ParentNonExist"
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
    @CacheEvict(value = CacheConstants.TENANT_ROLES, allEntries = true)
    public void update(TenantRolePrivate role) {
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
    @CacheEvict(value = CacheConstants.TENANT_ROLES, allEntries = true)
    public void delete(long id) {
        TenantRolePrivate role = SpringContextHolder.getBean(TenantRolePrivateService.class).getById(id);
        if (role == null) {
            return;
        }

        // 叶子才可以删除
        boolean result = count(Wrappers.<TenantRolePrivate>lambdaQuery()
                .eq(TenantRolePrivate::getPid, id)) == 0;
        assertionChecker.checkOperation(result,
                "TenantRolePrivateServiceImpl.RemoveFailedMustLeaf");

        removeById(id);
    }
}
