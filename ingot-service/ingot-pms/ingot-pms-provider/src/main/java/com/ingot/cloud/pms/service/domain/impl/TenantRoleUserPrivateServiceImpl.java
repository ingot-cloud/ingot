package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.cloud.pms.mapper.TenantRoleUserPrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
public class TenantRoleUserPrivateServiceImpl extends BaseServiceImpl<TenantRoleUserPrivateMapper, TenantRoleUserPrivate> implements TenantRoleUserPrivateService {

    @Override
    public List<TenantRoleUserPrivate> getUserRoles(long userId) {
        return CollUtil.emptyIfNull(list(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getUserId, userId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(BizBindDTO params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getBindIds();
        List<Long> removeIds = params.getRemoveIds();
        boolean metaFlag = params.isMetaFlag();

        if (CollUtil.isNotEmpty(removeIds)) {
            remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                    .eq(TenantRoleUserPrivate::getRoleId, roleId)
                    .in(TenantRoleUserPrivate::getUserId, removeIds));
        }

        List<TenantRoleUserPrivate> bindList = CollUtil.emptyIfNull(bindIds).stream()
                .map(userId -> {
                    TenantRoleUserPrivate bind = new TenantRoleUserPrivate();
                    bind.setRoleId(roleId);
                    bind.setUserId(userId);
                    bind.setMetaRole(metaFlag);
                    return bind;
                }).toList();
        if (CollUtil.isNotEmpty(bindList)) {
            saveBatch(bindList);
        }
    }

    @Override
    public void clearByRoleId(long id) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getRoleId, id));
    }
}
