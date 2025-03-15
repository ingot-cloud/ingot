package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.dto.application.ApplicationFilterDTO;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationPageItemVO;
import com.ingot.cloud.pms.mapper.SysApplicationMapper;
import com.ingot.cloud.pms.service.domain.SysApplicationService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-11-23
 */
@Service
public class SysApplicationServiceImpl extends BaseServiceImpl<SysApplicationMapper, SysApplication> implements SysApplicationService {

    @Override
    public IPage<ApplicationPageItemVO> page(Page<SysApplication> page, ApplicationFilterDTO filter) {
        return baseMapper.page(page, filter);
    }
}
