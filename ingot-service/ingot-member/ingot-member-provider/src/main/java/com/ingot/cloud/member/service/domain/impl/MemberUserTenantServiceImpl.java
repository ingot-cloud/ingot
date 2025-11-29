package com.ingot.cloud.member.service.domain.impl;

import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.cloud.member.mapper.MemberUserTenantMapper;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Service
public class MemberUserTenantServiceImpl extends BaseServiceImpl<MemberUserTenantMapper, MemberUserTenant> implements MemberUserTenantService {

}
