package com.ingot.cloud.member.service.domain.impl;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.mapper.MemberPermissionMapper;
import com.ingot.cloud.member.service.domain.MemberPermissionService;
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
public class MemberPermissionServiceImpl extends BaseServiceImpl<MemberPermissionMapper, MemberPermission> implements MemberPermissionService {

}
