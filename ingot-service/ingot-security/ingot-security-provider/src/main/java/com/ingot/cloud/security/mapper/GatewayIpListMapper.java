package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.GatewayIpList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 黑白名单 Mapper。
 *
 * @author jy
 * @since 2026/5/26
 */
@Mapper
public interface GatewayIpListMapper extends BaseMapper<GatewayIpList> {
}
