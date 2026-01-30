package com.ingot.framework.security.credential.data.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.framework.security.credential.data.mapper.PasswordHistoryMapper;
import com.ingot.framework.security.credential.model.domain.PasswordHistory;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 密码历史服务实现（通用，基于 MyBatis-Plus）
 *
 * @author jymot
 * @since 2026-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordHistoryServiceImpl implements PasswordHistoryService {

    private final PasswordHistoryMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PasswordHistory> getRecentHistory(Long userId, int limit) {
        log.debug("获取用户密码历史 - userId: {}, limit: {}", userId, limit);
        
        return mapper.selectList(
            Wrappers.<PasswordHistory>lambdaQuery()
                .eq(PasswordHistory::getUserId, userId)
                .orderByDesc(PasswordHistory::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveHistory(Long userId, String passwordHash, int maxRecords) {
        log.debug("保存密码历史 - userId: {}, maxRecords: {}", userId, maxRecords);
        
        // 查询当前用户的记录数量
        long count = mapper.selectCount(
            Wrappers.<PasswordHistory>lambdaQuery()
                .eq(PasswordHistory::getUserId, userId)
        );
        
        // 计算下一个序号（环形缓冲：1, 2, 3, ... maxRecords, 1, 2, ...）
        int nextSeq = (int)((count % maxRecords) + 1);
        
        log.debug("环形缓冲序号 - count: {}, nextSeq: {}", count, nextSeq);
        
        // 查找是否已存在该序号的记录
        PasswordHistory existing = mapper.selectOne(
            Wrappers.<PasswordHistory>lambdaQuery()
                .eq(PasswordHistory::getUserId, userId)
                .eq(PasswordHistory::getSequenceNumber, nextSeq)
        );
        
        if (existing != null) {
            // 更新现有记录（覆盖最旧的）
            existing.setPasswordHash(passwordHash);
            existing.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(existing);
            log.debug("更新密码历史记录 - id: {}, seq: {}", existing.getId(), nextSeq);
        } else {
            // 插入新记录
            PasswordHistory history = new PasswordHistory();
            history.setUserId(userId);
            history.setPasswordHash(passwordHash);
            history.setSequenceNumber(nextSeq);
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            mapper.insert(history);
            log.debug("插入密码历史记录 - seq: {}", nextSeq);
        }
    }

    @Override
    public boolean isPasswordUsed(Long userId, String passwordHash, int checkCount) {
        log.debug("检查密码是否已使用 - userId: {}, checkCount: {}", userId, checkCount);
        
        List<PasswordHistory> histories = getRecentHistory(userId, checkCount);
        
        boolean used = histories.stream()
            .anyMatch(h -> passwordEncoder.matches(passwordHash, h.getPasswordHash()));
        
        log.debug("密码检查结果 - used: {}", used);
        return used;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserId(Long userId) {
        log.info("删除用户密码历史 - userId: {}", userId);
        
        mapper.delete(
            Wrappers.<PasswordHistory>lambdaQuery()
                .eq(PasswordHistory::getUserId, userId)
        );
    }
}
