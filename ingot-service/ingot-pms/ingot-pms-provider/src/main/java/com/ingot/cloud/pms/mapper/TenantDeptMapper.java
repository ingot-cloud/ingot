package com.ingot.cloud.pms.mapper;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithMemberCountDTO;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;


import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Mapper
public interface TenantDeptMapper extends BaseMapper<TenantDept> {
    /**
     * 获取部门列表，并且返回部门成员数量
     *
     * @return {@link DeptWithMemberCountDTO}
     */
    List<DeptWithMemberCountDTO> listWithMemberCount();
}
