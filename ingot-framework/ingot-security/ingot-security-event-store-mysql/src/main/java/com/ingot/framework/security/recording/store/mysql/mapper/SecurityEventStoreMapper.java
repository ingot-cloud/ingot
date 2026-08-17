package com.ingot.framework.security.recording.store.mysql.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.framework.security.recording.model.SecurityEventQuery;
import com.ingot.framework.security.recording.store.mysql.model.CanonicalSecurityEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>canonical {@code security_event} 批量写入、游标查询与 retention 专用 Mapper。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface SecurityEventStoreMapper extends BaseMapper<CanonicalSecurityEventEntity> {

    int insertBatch(@Param("records") List<CanonicalSecurityEventEntity> records);

    List<CanonicalSecurityEventEntity> selectByCursor(
            @Param("query") SecurityEventQuery query,
            @Param("receivedFrom") LocalDateTime receivedFrom,
            @Param("receivedTo") LocalDateTime receivedTo,
            @Param("cursorReceivedAt") LocalDateTime cursorReceivedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);


    List<Long> selectExpiredIds(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit);

    int deleteByIds(@Param("ids") List<Long> ids);

    Integer acquireNamedLock(@Param("lockName") String lockName);

    Integer releaseNamedLock(@Param("lockName") String lockName);
}
