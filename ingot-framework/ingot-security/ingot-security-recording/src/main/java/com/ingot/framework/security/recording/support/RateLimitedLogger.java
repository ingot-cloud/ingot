package com.ingot.framework.security.recording.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>限流日志，避免对每个 dropped event 输出 warn。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RateLimitedLogger {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedLogger.class);

    private final String tag;
    private final AtomicLong counter = new AtomicLong();

    public RateLimitedLogger(String tag) {
        this.tag = tag;
    }

    public void warnOnceThenEvery(String message, long every) {
        long count = counter.incrementAndGet();
        if (count == 1 || count % every == 0) {
            log.warn("[{}] {} (occurrences={})", tag, message, count);
        }
    }
}
