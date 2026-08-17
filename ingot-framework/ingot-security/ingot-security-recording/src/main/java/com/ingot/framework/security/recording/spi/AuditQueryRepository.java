package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.AuditRecord;
import com.ingot.framework.security.recording.model.CursorPage;
import com.ingot.framework.security.recording.model.CursorPageRequest;

/**
 * <p>审计记录游标查询 SPI 预留契约。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AuditQueryRepository {

    CursorPage<AuditRecord> query(AuditQuery query, CursorPageRequest page);

    /**
     * <p>审计查询条件占位类型，具体字段由后续审计 change 定义。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    record AuditQuery() {
    }
}
