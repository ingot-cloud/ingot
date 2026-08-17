package com.ingot.framework.security.recording.model;

/**
 * <p>Transport 批量投递结果，区分可重试与终态失败。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param success 整批是否视为成功（含幂等重复）
 * @param acceptedCount 对端确认接纳条数
 * @param failedCount 失败条数
 * @param retryable 是否建议 caller 保留 claim 并重试
 * @param message 可选诊断信息
 */
public record DeliveryResult(
        boolean success,
        int acceptedCount,
        int failedCount,
        boolean retryable,
        String message) {

    public static DeliveryResult success(int count) {
        return new DeliveryResult(true, count, 0, false, null);
    }

    public static DeliveryResult failure(int failedCount, boolean retryable, String message) {
        return new DeliveryResult(false, 0, failedCount, retryable, message);
    }
}
