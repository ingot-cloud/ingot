package com.ingot.cloud.pms.service.domain.impl;

import java.util.Collection;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantUserDeptPrivate;
import com.ingot.cloud.pms.mapper.TenantUserDeptPrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantUserDeptPrivateService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-17
 */
@Service
public class TenantUserDeptPrivateServiceImpl extends BaseServiceImpl<TenantUserDeptPrivateMapper, TenantUserDeptPrivate> implements TenantUserDeptPrivateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDepartments(long userId, Collection<Long> deptIds) {
        // 先清空部门
        remove(Wrappers.<TenantUserDeptPrivate>lambdaQuery()
                .eq(TenantUserDeptPrivate::getUserId, userId));

        if (CollUtil.isEmpty(deptIds)) {
            return;
        }

        if (CollUtil.size(deptIds) == 1) {
            TenantUserDeptPrivate item = new TenantUserDeptPrivate();
            item.setUserId(userId);
            deptIds.stream().findFirst().ifPresent(item::setDeptId);
            save(item);
            return;
        }

        List<TenantUserDeptPrivate> list = deptIds.stream()
                .map(deptId -> {
                    TenantUserDeptPrivate item = new TenantUserDeptPrivate();
                    item.setUserId(userId);
                    item.setDeptId(deptId);
                    return item;
                }).toList();

        saveBatch(list);
    }

    @Override
    public List<Long> getUserDepartmentIds(long userId) {
        return CollUtil.emptyIfNull(list(Wrappers.<TenantUserDeptPrivate>lambdaQuery()
                        .eq(TenantUserDeptPrivate::getUserId, userId)))
                .stream()
                .map(TenantUserDeptPrivate::getDeptId)
                .toList();
    }

    @Override
    public void clearByUserId(long userId) {
        remove(Wrappers.<TenantUserDeptPrivate>lambdaQuery()
                .eq(TenantUserDeptPrivate::getUserId, userId));
    }
}
