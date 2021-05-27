package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.mapper.SysRoleUserMapper;
import com.ingot.cloud.pms.service.SysRoleUserService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysRoleUserServiceImpl extends BaseServiceImpl<SysRoleUserMapper, SysRoleUser> implements SysRoleUserService {
    private static final int BIND_TYPE_ROLE = 1;
    private static final int BIND_TYPE_USER = 2;

    private final I18nService i18nService;

    @Override
    public boolean removeByUserId(long userId) {
        return remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, userId));
    }

    @Override
    public boolean updateUserRole(long userId, List<Long> roles) {
        boolean result = removeByUserId(userId);
        if (!result) {
            return false;
        }
        if (CollUtil.isEmpty(roles)) {
            return true;
        }
        return roles.stream().allMatch(roleId -> {
            SysRoleUser entity = new SysRoleUser();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            return entity.insert();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userBindRoles(RelationDto<Long, Long> params) {
        commonRemove(BIND_TYPE_USER, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(RelationDto<Long, Long> params) {
        commonRemove(BIND_TYPE_ROLE, params);
    }

    private void commonRemove(int type, RelationDto<Long, Long> params) {
        Long id = params.getId();
        List<Long> removeIds = params.getRemoveIds();
        List<Long> bindIds = params.getBindIds();

        if (CollUtil.isNotEmpty(removeIds)) {
            boolean removeRet = true;
            switch (type) {
                case BIND_TYPE_ROLE:
                    removeRet = removeIds.stream().allMatch(userId ->
                            remove(Wrappers.<SysRoleUser>lambdaQuery()
                                    .eq(SysRoleUser::getRoleId, id)
                                    .eq(SysRoleUser::getUserId, userId)));
                    break;
                case BIND_TYPE_USER:
                    removeRet = removeIds.stream().allMatch(roleId ->
                            remove(Wrappers.<SysRoleUser>lambdaQuery()
                                    .eq(SysRoleUser::getUserId, id)
                                    .eq(SysRoleUser::getRoleId, roleId)));
                    break;
            }
            AssertionUtils.checkOperation(removeRet,
                    i18nService.getMessage("SysRoleUserServiceImpl.RemoveFailed"));
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            switch (type) {
                case BIND_TYPE_ROLE:
                    bindIds.forEach(userId -> getBaseMapper().insertIgnore(id, userId));
                    break;
                case BIND_TYPE_USER:
                    bindIds.forEach(roleId -> getBaseMapper().insertIgnore(roleId, id));
                    break;
            }
        }

    }
}
