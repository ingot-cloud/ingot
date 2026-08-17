package com.ingot.framework.security.recording.model;

import java.util.List;

/**
 * <p>游标分页结果，{@code nextCursor} 为 null 表示无更多数据。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 当前页记录
 * @param nextCursorReceivedAt 下一页游标 receivedAt
 * @param nextCursorId 下一页游标 id
 * @param hasMore 是否还有下一页
 * @param <T> 记录类型
 */
public record CursorPage<T>(
        List<T> items,
        java.time.Instant nextCursorReceivedAt,
        Long nextCursorId,
        boolean hasMore) {

    public CursorPage {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), null, null, false);
    }
}
