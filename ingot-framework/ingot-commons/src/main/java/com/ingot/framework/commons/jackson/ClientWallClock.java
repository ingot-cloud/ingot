package com.ingot.framework.commons.jackson;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import cn.hutool.core.date.DatePattern;

/**
 * <p>接口墙钟字面量：不带偏移，按请求时区解释；识别不到用 Asia/Shanghai。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ClientWallClock {
    /**
     * 接口墙钟格式，与 {@code InJavaTimeModule} 一致。
     */
    public static final String PATTERN = DatePattern.NORM_DATETIME_PATTERN;
    /**
     * 识别不到客户端时区时使用的区 ID。
     */
    public static final String ZONE_SHANGHAI = "Asia/Shanghai";
    /**
     * 墙钟解析器。
     */
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);
    /**
     * 缺省墙钟时区。
     */
    public static final ZoneId FALLBACK_ZONE = ZoneId.of(ZONE_SHANGHAI);
    /**
     * Jackson 无法识别时区时落到的 GMT ID。
     */
    public static final String ZONE_GMT = "GMT";
    /**
     * 无效的北京时区 ID，不得用于解释墙钟。
     */
    public static final String ZONE_BEIJING = "Asia/Beijing";

    private ClientWallClock() {
    }

    /**
     * 从 Jackson 上下文取区；无效或 GMT 回落上海。
     *
     * @param timeZone Jackson 当前时区，可空
     * @return 用于解释墙钟的区
     */
    public static ZoneId zoneOf(TimeZone timeZone) {
        if (timeZone == null) {
            return FALLBACK_ZONE;
        }
        String id = timeZone.getID();
        if (id == null || ZONE_GMT.equals(id) || ZONE_BEIJING.equals(id)) {
            return FALLBACK_ZONE;
        }
        try {
            return timeZone.toZoneId();
        } catch (DateTimeException exception) {
            return FALLBACK_ZONE;
        }
    }
}
