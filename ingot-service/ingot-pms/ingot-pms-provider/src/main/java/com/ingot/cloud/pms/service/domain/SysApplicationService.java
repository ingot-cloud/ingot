package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.dto.application.ApplicationFilterDTO;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationPageItemVO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jymot
 * @since 2023-11-23
 */
public interface SysApplicationService extends BaseService<SysApplication> {

    /**
     * 应用分页
     *
     * @param page   分页参数
     * @param filter 过滤参数
     * @return {@link ApplicationPageItemVO}
     */
    IPage<ApplicationPageItemVO> page(Page<SysApplication> page, ApplicationFilterDTO filter);
}
