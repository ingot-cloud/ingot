package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.mapper.AppRoleMapper;
import com.ingot.cloud.pms.service.domain.AppRoleService;
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
public class AppRoleServiceImpl extends BaseServiceImpl<AppRoleMapper, AppRole> implements AppRoleService {

}
