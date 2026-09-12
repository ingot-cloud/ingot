package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.PlatformResource;
import com.ingot.cloud.pms.mapper.PlatformResourceMapper;
import com.ingot.cloud.pms.service.domain.PlatformResourceService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>{@link PlatformResourceService} 的默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class PlatformResourceServiceImpl
        extends BaseServiceImpl<PlatformResourceMapper, PlatformResource>
        implements PlatformResourceService {
}
