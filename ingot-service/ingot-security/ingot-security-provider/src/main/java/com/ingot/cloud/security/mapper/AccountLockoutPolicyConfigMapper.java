package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.AccountLockoutPolicyConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>账号锁定策略表 Mapper。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface AccountLockoutPolicyConfigMapper extends BaseMapper<AccountLockoutPolicyConfig> {
}
