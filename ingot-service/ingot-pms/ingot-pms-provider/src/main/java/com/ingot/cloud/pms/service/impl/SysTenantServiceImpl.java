package com.ingot.cloud.pms.service.impl;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.mapper.SysTenantMapper;
import com.ingot.cloud.pms.service.SysTenantService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
public class SysTenantServiceImpl extends BaseServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {

}
