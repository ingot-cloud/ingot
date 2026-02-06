package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.CredentialPolicyConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭证策略配置 Mapper
 *
 * @author jymot
 * @since 2026-01-22
 */
@Mapper
public interface CredentialPolicyConfigMapper extends BaseMapper<CredentialPolicyConfig> {
}
