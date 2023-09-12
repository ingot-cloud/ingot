package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.mapper.AppUserTenantMapper;
import com.ingot.cloud.pms.service.domain.AppUserTenantService;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Service
public class AppUserTenantServiceImpl extends BaseServiceImpl<AppUserTenantMapper, AppUserTenant> implements AppUserTenantService {

}
