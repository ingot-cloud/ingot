package com.ingot.cloud.test.mapper;

import com.ingot.cloud.test.model.domain.TStudent;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2025-03-31
 */
@Mapper
public interface TStudentMapper extends BaseMapper<TStudent> {

    List<TStudent> studentList();
}
