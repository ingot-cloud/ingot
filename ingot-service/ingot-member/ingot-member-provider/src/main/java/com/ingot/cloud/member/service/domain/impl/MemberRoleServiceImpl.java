package com.ingot.cloud.member.service.domain.impl;

import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.mapper.MemberRoleMapper;
import com.ingot.cloud.member.service.domain.MemberRoleService;
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
public class MemberRoleServiceImpl extends BaseServiceImpl<MemberRoleMapper, MemberRole> implements MemberRoleService {

}
