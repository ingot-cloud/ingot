package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUserDept;
import com.ingot.cloud.pms.mapper.SysUserDeptMapper;
import com.ingot.cloud.pms.service.domain.SysUserDeptService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-09-13
 */
@Service
public class SysUserDeptServiceImpl extends BaseServiceImpl<SysUserDeptMapper, SysUserDept> implements SysUserDeptService {

    @Override
    public SysUserDept getByUserIdAndTenant(long userId, long tenantId) {
        return getOne(Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getUserId, userId)
                .eq(SysUserDept::getTenantId, tenantId));
    }
}
