package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import com.ingot.cloud.iam.infrastructure.persistence.entity.IdAllocationEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2023-10-24
 */
@Mapper
public interface IdAllocationMapper extends BaseMapper<IdAllocationEntity> {

}
