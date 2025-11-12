package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaRoleAuthority;
import com.ingot.cloud.pms.mapper.MetaRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.MetaRoleAuthorityService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
public class MetaRoleAuthorityServiceImpl extends BaseServiceImpl<MetaRoleAuthorityMapper, MetaRoleAuthority> implements MetaRoleAuthorityService {

    @Override
    public void clearByAuthorityId(long authorityId) {
        remove(Wrappers.<MetaRoleAuthority>lambdaQuery()
                .eq(MetaRoleAuthority::getAuthorityId, authorityId));
    }
}
