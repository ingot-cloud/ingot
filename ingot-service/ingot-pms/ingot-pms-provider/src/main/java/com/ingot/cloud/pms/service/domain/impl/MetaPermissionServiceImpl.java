package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.MetaPermissionMapper;
import com.ingot.cloud.pms.service.domain.MetaPermissionService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
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
public class MetaPermissionServiceImpl extends BaseServiceImpl<MetaPermissionMapper, MetaPermission> implements MetaPermissionService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.META_PERMISSIONS, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<MetaPermission> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.META_PERMISSIONS, key = CacheKey.ItemKey, unless = "#result == null")
    public MetaPermission getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_PERMISSIONS, allEntries = true)
    public void create(MetaPermission authority, boolean fillParentCode) {
        createAndReturnId(authority, fillParentCode);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_PERMISSIONS, allEntries = true)
    public Long createAndReturnId(MetaPermission authority, boolean fillParentCode) {
        assertionChecker.checkOperation(authority.getType() != null,
                "MetaAuthorityServiceImpl.TypeNonNull");
        assertionChecker.checkOperation(authority.getOrgType() != null,
                "MetaAuthorityServiceImpl.OrgTypeNonNull");

        // 检测父权限，填充编码
        if (fillParentCode) {
            authority.setCode(formatCode(authority.getPid(), authority.getCode()));
        }

        // code 不能重复
        assertionChecker.checkOperation(count(Wrappers.<MetaPermission>lambdaQuery()
                        .eq(MetaPermission::getCode, authority.getCode())) == 0,
                "MetaAuthorityServiceImpl.ExistCode");

        if (authority.getStatus() == null) {
            authority.setStatus(CommonStatusEnum.ENABLE);
        }

        authority.setCreatedAt(DateUtil.now());
        authority.setUpdatedAt(authority.getCreatedAt());

        save(authority);
        return authority.getId();
    }

    @Override
    @CacheEvict(value = CacheConstants.META_PERMISSIONS, allEntries = true)
    public void update(MetaPermission authority) {
        // 权限编码不可更新
        authority.setCode(null);
        authority.setUpdatedAt(DateUtil.now());
        updateById(authority);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_PERMISSIONS, allEntries = true)
    public void delete(long id) {
        // 叶子才可以删除
        boolean result = count(Wrappers.<MetaPermission>lambdaQuery()
                .eq(MetaPermission::getPid, id)) == 0;
        assertionChecker.checkOperation(result,
                "MetaAuthorityServiceImpl.RemoveFailedMustLeaf");

        removeById(id);
    }

    private String formatCode(Long pid, String code) {
        return Optional.ofNullable(pid)
                .filter(id -> id != IDConstants.ROOT_TREE_ID)
                .map(this::getById)
                .map(item -> item.getCode() + StrUtil.COLON + code)
                .orElse(code);
    }
}
