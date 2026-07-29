package com.ingot.framework.security.account.adapter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.framework.security.account.adapter.entity.AccountSecurityEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安全事件 Mapper
 *
 * @author jymot
 * @since 2026-02-13
 */
@Mapper
public interface AccountSecurityEventMapper extends BaseMapper<AccountSecurityEventEntity> {
}
