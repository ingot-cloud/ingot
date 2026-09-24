package com.ingot.framework.commons.jackson;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * <p>把接口墙钟或 ISO-8601 解析为 Instant；空白视为空。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class WallClockInstantDeserializer extends JsonDeserializer<Instant> {
    /**
     * 解析墙钟或瞬时字面量。
     *
     * @param parser 当前 JSON 解析器
     * @param context 反序列化上下文
     * @return Instant；空白返回 {@code null}
     * @throws IOException 字面量无法解析
     */
    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        if (value.indexOf('T') >= 0) {
            return Instant.parse(value);
        }
        try {
            LocalDateTime local = LocalDateTime.parse(value, ClientWallClock.FORMATTER);
            return local.atZone(ClientWallClock.zoneOf(context.getTimeZone())).toInstant();
        } catch (DateTimeParseException exception) {
            throw context.weirdStringException(value, Instant.class, "不是 yyyy-MM-dd HH:mm:ss 或 ISO-8601");
        }
    }
}
