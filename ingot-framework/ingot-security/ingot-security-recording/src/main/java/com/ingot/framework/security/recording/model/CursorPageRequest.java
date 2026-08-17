package com.ingot.framework.security.recording.model;

/**
 * <p>游标分页请求，禁止 offset 深分页。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param size 页大小，默认 50，最大 200
 * @param cursorReceivedAt 上一页最后一条的 receivedAt，首页为 null
 * @param cursorId 上一页最后一条的物理 id，首页为 null
 */
public record CursorPageRequest(
        int size,
        java.time.Instant cursorReceivedAt,
        Long cursorId) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public CursorPageRequest {
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        size = Math.min(size, MAX_SIZE);
    }

    public static CursorPageRequest firstPage(int size) {
        return new CursorPageRequest(size, null, null);
    }
}
