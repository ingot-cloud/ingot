package com.ingot.framework.commons.jackson;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * <p>把 Instant 写成不带偏移的接口墙钟。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class WallClockInstantSerializer extends JsonSerializer<Instant> {
    /**
     * 按请求时区写出墙钟字符串。
     *
     * @param value 瞬时，可空
     * @param generator JSON 写出器
     * @param serializers 序列化上下文
     * @throws IOException 写出失败
     */
    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        LocalDateTime local = LocalDateTime.ofInstant(value, ClientWallClock.zoneOf(serializers.getTimeZone()));
        generator.writeString(ClientWallClock.FORMATTER.format(local));
    }
}
