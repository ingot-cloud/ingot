package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.GatewayEndpointGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 路径分组 Mapper。
 *
 * @author jy
 * @since 2026/5/26
 */
@Mapper
public interface GatewayEndpointGroupMapper extends BaseMapper<GatewayEndpointGroup> {
}
