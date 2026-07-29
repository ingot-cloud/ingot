package com.ingot.cloud.security.service;

/**
 * 中心 {@code security_event} 过期清理服务。
 */
public interface SecurityEventRetentionService {

    /**
     * 物理删除早于保留期的记录。
     *
     * @return 本次任务删除的总条数
     */
    int purgeExpired();
}
