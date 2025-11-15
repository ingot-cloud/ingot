package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.mapper.MetaAppMapper;
import com.ingot.cloud.pms.service.domain.MetaAppService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
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
public class MetaAppServiceImpl extends BaseServiceImpl<MetaAppMapper, MetaApp> implements MetaAppService {

    @Override
    public IPage<MetaApp> conditionPage(Page<MetaApp> pageParams, MetaApp condition) {
        return page(pageParams, Wrappers.lambdaQuery(condition));
    }

    @Override
    public void create(MetaApp params) {
        if (params.getStatus() == null) {
            params.setStatus(CommonStatusEnum.ENABLE);
        }

        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(params.getCreatedAt());
        save(params);
    }

    @Override
    public void update(MetaApp params) {
        params.setUpdatedAt(params.getCreatedAt());
        updateById(params);
    }

    @Override
    public void delete(long id) {
        removeById(id);
    }
}
