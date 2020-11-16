package com.ingot.cloud.pms.service.impl;

import com.ingot.cloud.pms.model.domain.SysUser;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-13
 */
@Service
@Transactional
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

}
