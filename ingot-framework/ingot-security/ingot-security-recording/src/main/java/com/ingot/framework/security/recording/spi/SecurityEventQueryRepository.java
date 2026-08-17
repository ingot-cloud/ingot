package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.CursorPage;
import com.ingot.framework.security.recording.model.CursorPageRequest;
import com.ingot.framework.security.recording.model.SecurityEventQuery;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

/**
 * <p>安全事件游标查询 SPI，仅 MySQL/未来 ES Store 注册。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface SecurityEventQueryRepository {

    CursorPage<SecurityEventRecord> query(SecurityEventQuery query, CursorPageRequest page);
}
