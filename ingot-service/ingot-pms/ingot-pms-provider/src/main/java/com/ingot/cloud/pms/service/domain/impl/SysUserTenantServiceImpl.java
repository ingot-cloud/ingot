package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.mapper.SysUserTenantMapper;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : SysUserTenantServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/2.</p>
 * <p>Time         : 12:07 PM.</p>
 */
@Service
public class SysUserTenantServiceImpl extends BaseServiceImpl<SysUserTenantMapper, SysUserTenant> implements SysUserTenantService {
}
