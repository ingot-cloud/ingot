package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.SecurityEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一安全事件 Mapper。
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface SecurityEventMapper extends BaseMapper<SecurityEvent> {
}
