package com.ingot.framework.commons.jackson;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * <p>API 时间点的严格 ISO-8601 解析与 UTC 边界转换，不依赖 JVM 或请求时区。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ApiTime {
    private ApiTime() {
    }

    /**
     * 解析带 Z 或偏移量的 ISO 时间点，无时区、旧墙钟格式及非法日期均失败。
     * @throws java.time.DateTimeException 输入不符合带偏移量的 ISO 格式
     */
    public static Instant parse(String value) {
        return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    }

    /** 将 API 时间点转换为持久化使用的 UTC LocalDateTime，保留纳秒。 */
    public static LocalDateTime parseUtc(String value) {
        return LocalDateTime.ofInstant(parse(value), ZoneOffset.UTC);
    }
}
