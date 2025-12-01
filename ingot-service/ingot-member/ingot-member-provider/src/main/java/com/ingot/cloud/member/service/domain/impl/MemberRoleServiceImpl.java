package com.ingot.cloud.member.service.domain.impl;

import java.io.Serializable;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.common.CacheKey;
import com.ingot.cloud.member.mapper.MemberRoleMapper;
import com.ingot.cloud.member.service.domain.MemberRoleService;
import com.ingot.framework.commons.constants.CacheConstants;
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
public class MemberRoleServiceImpl extends BaseServiceImpl<MemberRoleMapper, MemberRole> implements MemberRoleService {
    private final AssertionChecker assertionChecker;

    @Override
    @Cacheable(value = CacheConstants.MEMBER_ROLES, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<MemberRole> list() {
        return super.list();
    }

    @Override
    @Cacheable(value = CacheConstants.MEMBER_ROLES, key = CacheKey.ItemKey, unless = "#result == null")
    public MemberRole getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(value = CacheConstants.MEMBER_ROLES, key = CacheKey.CodeKey, unless = "#result == null")
    public MemberRole getByCode(String code) {
        return getOne(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getCode, code));
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_ROLES, allEntries = true)
    public void create(MemberRole role) {
        createAndReturnResult(role);
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_ROLES, allEntries = true)
    public MemberRole createAndReturnResult(MemberRole role) {
        assertionChecker.checkOperation(
                StrUtil.isNotEmpty(role.getCode()),
                "MemberRoleServiceImpl.CodeNonNull"
        );
        assertionChecker.checkOperation(
                count(Wrappers.<MemberRole>lambdaQuery()
                        .eq(MemberRole::getCode, role.getCode())) == 0,
                "MemberRoleServiceImpl.ExistCode"
        );
        assertionChecker.checkOperation(
                role.getPid() != null,
                count(Wrappers.<MemberRole>lambdaQuery()
                        .eq(MemberRole::getId, role.getPid())) > 0,
                "MemberRoleServiceImpl.ParentNonExist"
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
    @CacheEvict(value = CacheConstants.MEMBER_ROLES, allEntries = true)
    public void update(MemberRole role) {
        // 角色编码不可修改
        role.setCode(null);
        role.setBuiltIn(null);
        role.setUpdatedAt(DateUtil.now());
        updateById(role);
    }

    @Override
    @CacheEvict(value = CacheConstants.MEMBER_ROLES, allEntries = true)
    public void delete(long id) {
        // 叶子才可以删除
        boolean result = count(Wrappers.<MemberRole>lambdaQuery()
                .eq(MemberRole::getPid, id)) == 0;
        assertionChecker.checkOperation(result,
                "MemberRoleServiceImpl.RemoveFailedMustLeaf");

        removeById(id);
    }
}
