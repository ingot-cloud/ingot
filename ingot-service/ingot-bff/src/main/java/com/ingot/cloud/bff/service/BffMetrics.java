package com.ingot.cloud.bff.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 固定应用/错误分类的低基数指标，不使用用户、事务或会话秘密作为标签。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BffMetrics {

    private static final String APP_TAG = "app";

    private static final String RESULT_TAG = "result";

    private static final String EVENT_PREFIX = "ingot.bff.";

    private final MeterRegistry registry;

    private final ConcurrentHashMap<String, AtomicLong> backlogs = new ConcurrentHashMap<>();

    private static final int CORRELATION_LENGTH = 12;

    /** 只输出不可反推原事务的短摘要，用于关联故障，不把随机秘密写入日志。 */
    public String correlation(String transactionId) {
        return transactionId == null ? "unknown"
                : cn.hutool.crypto.digest.DigestUtil.sha256Hex(transactionId).substring(0, CORRELATION_LENGTH);
    }

    /** 记录准备/恢复/提交及失败分类。 */
    public void event(String operation, String appId, String result) {
        registry.counter(EVENT_PREFIX + operation, APP_TAG, appId, RESULT_TAG, result).increment();
    }

    /** 记录认证总耗时，标签不含账号或远端异常文本。 */
    public void authentication(String appId, long startedNanos) {
        registry.timer(EVENT_PREFIX + "authentication", APP_TAG, appId)
            .record(System.nanoTime() - startedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    /** 清理轮询同步实际队列长度，包括等待交付窗口的任务。 */
    public void backlog(String appId, long size) {
        backlogs
            .computeIfAbsent(appId,
                    key -> registry.gauge(EVENT_PREFIX + "cleanup.backlog",
                            io.micrometer.core.instrument.Tags.of(APP_TAG, key), new AtomicLong()))
            .set(size);
    }

}
