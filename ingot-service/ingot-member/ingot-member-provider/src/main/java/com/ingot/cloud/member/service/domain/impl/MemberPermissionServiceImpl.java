package com.ingot.cloud.member.service.domain.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.common.CacheKey;
import com.ingot.cloud.member.mapper.MemberPermissionMapper;
import com.ingot.cloud.member.service.domain.MemberPermissionService;
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
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
public class MemberPermissionServiceImpl extends BaseServiceImpl<MemberPermissionMapper, MemberPermission> implements MemberPermissionService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.MEMBER_PERMISSIONS, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<MemberPermission> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.MEMBER_PERMISSIONS, key = CacheKey.ItemKey, unless = "#result == null")
    public MemberPermission getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_PERMISSIONS, allEntries = true)
    public void create(MemberPermission authority, boolean fillParentCode) {
        createAndReturnId(authority, fillParentCode);
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_PERMISSIONS, allEntries = true)
    public Long createAndReturnId(MemberPermission authority, boolean fillParentCode) {
        assertionChecker.checkOperation(authority.getType() != null,
                "MemberPermissionServiceImpl.TypeNonNull");

        // 检测父权限，填充编码
        if (fillParentCode) {
            authority.setCode(formatCode(authority.getPid(), authority.getCode()));
        }

        // code 不能重复
        assertionChecker.checkOperation(count(Wrappers.<MemberPermission>lambdaQuery()
                        .eq(MemberPermission::getCode, authority.getCode())) == 0,
                "MemberPermissionServiceImpl.ExistCode");

        if (authority.getStatus() == null) {
            authority.setStatus(CommonStatusEnum.ENABLE);
        }

        authority.setCreatedAt(DateUtil.now());
        authority.setUpdatedAt(authority.getCreatedAt());

        save(authority);
        return authority.getId();
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_PERMISSIONS, allEntries = true)
    public void update(MemberPermission authority) {
        // 权限编码不可更新
        authority.setCode(null);
        authority.setUpdatedAt(DateUtil.now());
        updateById(authority);
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_PERMISSIONS, allEntries = true)
    public void delete(long id) {
        // 叶子才可以删除
        boolean result = count(Wrappers.<MemberPermission>lambdaQuery()
                .eq(MemberPermission::getPid, id)) == 0;
        assertionChecker.checkOperation(result,
                "MemberPermissionServiceImpl.RemoveFailedMustLeaf");

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
