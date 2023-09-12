package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.mapper.AppRoleUserMapper;
import com.ingot.cloud.pms.service.domain.AppRoleUserService;
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
public class AppRoleUserServiceImpl extends BaseServiceImpl<AppRoleUserMapper, AppRoleUser> implements AppRoleUserService {

}
