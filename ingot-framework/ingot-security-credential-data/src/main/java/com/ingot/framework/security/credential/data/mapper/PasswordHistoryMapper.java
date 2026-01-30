package com.ingot.framework.security.credential.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.framework.security.credential.model.domain.PasswordHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 密码历史 Mapper（通用）
 *
 * @author jymot
 * @since 2026-01-24
 */
@Mapper
public interface PasswordHistoryMapper extends BaseMapper<PasswordHistory> {
}
