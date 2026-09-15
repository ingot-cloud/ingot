package com.ingot.cloud.iam.service.domain.impl;

import com.ingot.cloud.iam.api.model.domain.PlatformResource;
import com.ingot.cloud.iam.mapper.PlatformResourceMapper;
import com.ingot.cloud.iam.service.domain.PlatformResourceService;
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
