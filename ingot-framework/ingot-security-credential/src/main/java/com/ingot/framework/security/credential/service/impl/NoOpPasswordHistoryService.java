package com.ingot.framework.security.credential.service.impl;

import java.util.Collections;
import java.util.List;

import com.ingot.framework.security.credential.model.domain.PasswordHistory;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认密码历史服务实现（空实现）
 * <p>不检查密码历史，适用于不需要历史检查的场景</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
@Slf4j
public class NoOpPasswordHistoryService implements PasswordHistoryService {

    @Override
    public List<PasswordHistory> getRecentHistory(Long userId, int limit) {
        log.debug("使用默认空实现，返回空历史列表");
        return Collections.emptyList();
    }

    @Override
    public void saveHistory(Long userId, String passwordHash, int maxRecords) {
        log.debug("使用默认空实现，不保存密码历史");
        // 空实现，不保存
    }

    @Override
    public boolean isPasswordUsed(Long userId, String password, int checkCount) {
        log.debug("使用默认空实现，密码未使用过");
        return false;
    }

    @Override
    public void deleteByUserId(Long userId) {
        log.debug("使用默认空实现，无需删除");
        // 空实现，无需删除
    }
}
