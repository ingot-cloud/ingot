package com.ingot.framework.security.recording.model;

/**
 * <p>RecordQueue 入队结果。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param accepted 是否成功接纳
 * @param reason 拒绝或失败原因
 */
public record EnqueueResult(boolean accepted, String reason) {

    public static EnqueueResult success() {
        return new EnqueueResult(true, null);
    }

    public static EnqueueResult rejected(String reason) {
        return new EnqueueResult(false, reason);
    }

    public boolean isAccepted() {
        return accepted;
    }
}
