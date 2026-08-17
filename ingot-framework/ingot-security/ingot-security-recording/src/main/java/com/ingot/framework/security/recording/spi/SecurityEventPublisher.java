package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

/**
 * <p>安全事件统一发布入口，业务模块唯一依赖的写侧契约。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see PublishOutcome
 */
public interface SecurityEventPublisher {

    /**
     * 发布单条安全事件；不抛出 Store/Transport 异常，默认 fail-open。
     *
     * @param record 待发布记录
     * @return 同步接纳结果
     */
    PublishOutcome publish(SecurityEventRecord record);
}
