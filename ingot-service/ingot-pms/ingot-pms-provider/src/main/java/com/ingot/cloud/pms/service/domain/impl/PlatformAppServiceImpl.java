package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.common.CacheKey;
import com.ingot.cloud.pms.mapper.PlatformAppMapper;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
public class PlatformAppServiceImpl extends BaseServiceImpl<PlatformAppMapper, PlatformApp> implements PlatformAppService {

    @Override
    @Cacheable(value = CacheConstants.PLATFORM_APPS, key = CacheKey.ListKey, unless = "#result.isEmpty()")
    public List<PlatformApp> list() {
        return super.list();
    }

    @Override
    public IPage<PlatformApp> conditionPage(Page<PlatformApp> pageParams, PlatformApp condition) {
        return page(pageParams, Wrappers.lambdaQuery(condition));
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_APPS, allEntries = true)
    public void create(PlatformApp params) {
        if (params.getStatus() == null) {
            params.setStatus(CommonStatusEnum.ENABLE);
        }

        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(params.getCreatedAt());
        save(params);
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_APPS, allEntries = true)
    public void update(PlatformApp params) {
        params.setUpdatedAt(params.getCreatedAt());
        updateById(params);
    }

    @Override
    @CacheEvict(value = CacheConstants.PLATFORM_APPS, allEntries = true)
    public void delete(long id) {
        removeById(id);
    }
}
