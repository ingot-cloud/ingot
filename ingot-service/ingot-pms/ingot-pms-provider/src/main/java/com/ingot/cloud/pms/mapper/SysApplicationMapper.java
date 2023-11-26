package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.dto.application.ApplicationFilterDTO;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationOrgPageItemVO;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationPageItemVO;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2023-11-23
 */
public interface SysApplicationMapper extends BaseMapper<SysApplication> {

    /**
     * 应用分页
     *
     * @param page   分页参数
     * @param filter 过滤参数
     * @return {@link ApplicationOrgPageItemVO}
     */
    IPage<ApplicationPageItemVO> page(Page<SysApplication> page, @Param("filter") ApplicationFilterDTO filter);
}
