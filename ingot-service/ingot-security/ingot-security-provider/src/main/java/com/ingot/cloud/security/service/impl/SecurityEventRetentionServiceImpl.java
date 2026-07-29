package com.ingot.cloud.security.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.config.SecurityEventProperties;
import com.ingot.cloud.security.api.support.SecurityEventRetentionSupport;
import com.ingot.cloud.security.mapper.SecurityEventMapper;
import com.ingot.cloud.security.model.domain.SecurityEvent;
import com.ingot.cloud.security.service.SecurityEventRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 中心 {@code security_event} 过期清理实现。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventRetentionServiceImpl implements SecurityEventRetentionService {

    private final SecurityEventMapper securityEventMapper;
    private final SecurityEventProperties properties;

    @Override
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
            log.info("[SecurityEventRetention] purged {} rows before {}", total, cutoff);
        }
        return total;
    }

    private int deleteBatch(LocalDateTime cutoff, int batchSize) {
        return securityEventMapper.delete(Wrappers.<SecurityEvent>lambdaQuery()
                .lt(SecurityEvent::getReceivedAt, cutoff)
                .last("LIMIT " + batchSize));
    }
}
