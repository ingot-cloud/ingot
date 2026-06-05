package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.GatewayRateLimitRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 限流规则 Mapper。
 *
 * @author jy
 * @since 2026/5/26
 */
@Mapper
public interface GatewayRateLimitRuleMapper extends BaseMapper<GatewayRateLimitRule> {
}
