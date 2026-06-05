package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.GatewayBlacklistEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 封禁审计事件 Mapper。
 *
 * @author jy
 * @since 2026/5/26
 */
@Mapper
public interface GatewayBlacklistEventMapper extends BaseMapper<GatewayBlacklistEvent> {
}
