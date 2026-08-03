package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.LoginFailureProtectionPolicy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录失败保护策略 Mapper。
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface LoginFailureProtectionPolicyMapper extends BaseMapper<LoginFailureProtectionPolicy> {
}
