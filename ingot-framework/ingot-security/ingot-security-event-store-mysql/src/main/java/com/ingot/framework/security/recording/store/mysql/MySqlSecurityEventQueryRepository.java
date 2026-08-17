package com.ingot.framework.security.recording.store.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.CursorPage;
import com.ingot.framework.security.recording.model.CursorPageRequest;
import com.ingot.framework.security.recording.model.SecurityEventQuery;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.SecurityEventQueryRepository;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventRecordConverter;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import com.ingot.framework.security.recording.store.mysql.model.CanonicalSecurityEventEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>MySQL 安全事件游标查询实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MySqlSecurityEventQueryRepository implements SecurityEventQueryRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final long DEFAULT_RANGE_HOURS = 24;

    private final SecurityEventStoreMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlSecurityEventQueryRepository(SecurityEventStoreMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public CursorPage<SecurityEventRecord> query(SecurityEventQuery query, CursorPageRequest page) {
        LocalDateTime receivedTo = query.receivedTo() == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(query.receivedTo(), ZONE);
        LocalDateTime receivedFrom = query.receivedFrom() == null
                ? receivedTo.minusHours(DEFAULT_RANGE_HOURS)
                : LocalDateTime.ofInstant(query.receivedFrom(), ZONE);
        long maxDays = SecurityEventQuery.MAX_RANGE_DAYS;
        LocalDateTime minAllowed = receivedTo.minusDays(maxDays);
        if (receivedFrom.isBefore(minAllowed)) {
            receivedFrom = minAllowed;
        }

        LocalDateTime cursorReceivedAt = page.cursorReceivedAt() == null
                ? null
                : LocalDateTime.ofInstant(page.cursorReceivedAt(), ZONE);
        int limit = page.size() + 1;
        List<CanonicalSecurityEventEntity> rows = mapper.selectByCursor(
                query,
                receivedFrom,
                receivedTo,
                cursorReceivedAt,
                page.cursorId(),
                limit);

        boolean hasMore = rows.size() > page.size();
        List<CanonicalSecurityEventEntity> pageRows = hasMore ? rows.subList(0, page.size()) : rows;
        List<SecurityEventRecord> items = new ArrayList<>(pageRows.size());
        for (CanonicalSecurityEventEntity row : pageRows) {
            items.add(SecurityEventRecordConverter.toRecord(row, objectMapper));
        }
        if (pageRows.isEmpty()) {
            return CursorPage.empty();
        }
        CanonicalSecurityEventEntity last = pageRows.get(pageRows.size() - 1);
        Instant nextReceivedAt = last.getReceivedAt().atZone(ZONE).toInstant().truncatedTo(ChronoUnit.SECONDS);
        return new CursorPage<>(items, nextReceivedAt, last.getId(), hasMore);
    }
}
