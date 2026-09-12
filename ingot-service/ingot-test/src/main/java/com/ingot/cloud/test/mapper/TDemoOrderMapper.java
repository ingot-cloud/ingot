package com.ingot.cloud.test.mapper;

import com.ingot.cloud.test.model.domain.TDemoOrder;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>示例订单 Mapper。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface TDemoOrderMapper extends BaseMapper<TDemoOrder> {
}
