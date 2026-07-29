package com.ingot.framework.account.adapter.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.cloud.security.api.support.SecurityEventRetentionSupport;
import com.ingot.framework.account.adapter.entity.AccountSecurityEventEntity;
import com.ingot.framework.account.adapter.mapper.AccountSecurityEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 本地 {@code account_security_event} 过期清理。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSecurityEventRetentionService {

    private final AccountSecurityEventMapper eventMapper;
    private final SecurityEventProperties properties;

    /**
     * 物理删除早于保留期的记录。
     *
     * @return 本次任务删除的总条数
     */
    public int purgeExpired() {
        if (!properties.isRetentionActive()) {
            return 0;
        }
        SecurityEventProperties.Retention retention = properties.getRetention();
        LocalDateTime cutoff = SecurityEventRetentionSupport.resolveCutoff(retention);
        if (cutoff == null) {
            return 0;
        }
        int batchSize = SecurityEventRetentionSupport.resolveBatchSize(retention);
        int maxRounds = SecurityEventRetentionSupport.resolveMaxRounds(retention);
        int total = 0;
        for (int round = 0; round < maxRounds; round++) {
            int deleted = deleteBatch(cutoff, batchSize);
            total += deleted;
            if (deleted <= 0) {
                break;
            }
        }
        if (total > 0) {
            log.info("[AccountSecurityEventRetention] purged {} rows before {}", total, cutoff);
        }
        return total;
    }

    private int deleteBatch(LocalDateTime cutoff, int batchSize) {
        return eventMapper.delete(Wrappers.<AccountSecurityEventEntity>lambdaQuery()
                .lt(AccountSecurityEventEntity::getCreatedAt, cutoff)
                .last("LIMIT " + batchSize));
    }
}
