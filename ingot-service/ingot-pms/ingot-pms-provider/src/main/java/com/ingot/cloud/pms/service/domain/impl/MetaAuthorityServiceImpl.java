package com.ingot.cloud.pms.service.domain.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.MetaAuthorityMapper;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
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
public class MetaAuthorityServiceImpl extends BaseServiceImpl<MetaAuthorityMapper, MetaAuthority> implements MetaAuthorityService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.META_AUTHORITIES, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<MetaAuthority> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.META_AUTHORITIES, key = CacheKey.ItemKey, unless = "#result == null")
    public MetaAuthority getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_AUTHORITIES, allEntries = true)
    public void create(MetaAuthority authority, boolean fillParentCode) {
        createAndReturnId(authority, fillParentCode);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_AUTHORITIES, allEntries = true)
    public Long createAndReturnId(MetaAuthority authority, boolean fillParentCode) {
        assertionChecker.checkOperation(authority.getType() != null,
                "MetaAuthorityServiceImpl.TypeNonNull");
        assertionChecker.checkOperation(authority.getOrgType() != null,
                "MetaAuthorityServiceImpl.OrgTypeNonNull");

        // 检测父权限，填充编码
        if (fillParentCode) {
            authority.setCode(formatCode(authority.getPid(), authority.getCode()));
        }

        // code 不能重复
        assertionChecker.checkOperation(count(Wrappers.<MetaAuthority>lambdaQuery()
                        .eq(MetaAuthority::getCode, authority.getCode())) == 0,
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
    @CacheEvict(value = CacheConstants.META_AUTHORITIES, allEntries = true)
    public void update(MetaAuthority authority) {
        // 权限编码不可更新
        authority.setCode(null);
        authority.setUpdatedAt(DateUtil.now());
        updateById(authority);
    }

    @Override
    @CacheEvict(value = CacheConstants.META_AUTHORITIES, allEntries = true)
    public void delete(long id) {
        // 叶子才可以删除
        boolean result = count(Wrappers.<MetaAuthority>lambdaQuery()
                .eq(MetaAuthority::getPid, id)) == 0;
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
