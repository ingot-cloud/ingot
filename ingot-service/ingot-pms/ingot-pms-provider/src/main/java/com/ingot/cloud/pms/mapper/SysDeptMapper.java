package com.ingot.cloud.pms.mapper;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithMemberCountDTO;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 获取部门列表，并且返回部门成员数量
     *
     * @return {@link DeptWithMemberCountDTO}
     */
    List<DeptWithMemberCountDTO> listWithMemberCount();
}
