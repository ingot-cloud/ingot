package com.ingot.framework.security.recording.spi;

/**
 * <p>安全事件 retention 清理 SPI，由 MySQL Store module 实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface SecurityEventRetentionHandler {

    /**
     * 执行一轮 retention，返回本实例删除条数。
     *
     * @return 删除条数；获取锁失败或写入积压让步时可为 0
     */
    int runRetentionRound();
}
