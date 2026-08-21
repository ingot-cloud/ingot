package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.SessionConcurrencyPolicy;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>并发会话策略 Mapper。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface SessionConcurrencyPolicyMapper extends BaseMapper<SessionConcurrencyPolicy> {
}
