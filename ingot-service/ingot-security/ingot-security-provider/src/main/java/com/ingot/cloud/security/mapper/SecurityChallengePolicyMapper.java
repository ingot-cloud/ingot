package com.ingot.cloud.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 挑战策略 Mapper。
 *
 * @author jy
 * @since 2026/5/26
 */
@Mapper
public interface SecurityChallengePolicyMapper extends BaseMapper<SecurityChallengePolicy> {
}
