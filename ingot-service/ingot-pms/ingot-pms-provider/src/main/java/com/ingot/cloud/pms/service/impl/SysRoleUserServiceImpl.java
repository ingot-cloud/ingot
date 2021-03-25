package com.ingot.cloud.pms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.mapper.SysRoleUserMapper;
import com.ingot.cloud.pms.service.SysRoleUserService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

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
public class SysRoleUserServiceImpl extends BaseServiceImpl<SysRoleUserMapper, SysRoleUser> implements SysRoleUserService {

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
        return roles.stream().allMatch(roleId -> {
            SysRoleUser entity = new SysRoleUser();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            return entity.insert();
        });
    }
}
