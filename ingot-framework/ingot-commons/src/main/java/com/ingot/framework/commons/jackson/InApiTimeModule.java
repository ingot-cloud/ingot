package com.ingot.framework.commons.jackson;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Function;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

/**
 * <p>HTTP/Feign 专用时间编码：时间点收发 ISO，响应 UTC Z；不得装配到 Redis/OAuth mapper。</p>
 *
 * <p>LocalDateTime 时间点明确表示 UTC；日期和每天几点保留日历语义。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class InApiTimeModule extends SimpleModule {
    /** 装配 API 时间点及日期、日内时间编码。 */
    public InApiTimeModule() {
        super(InApiTimeModule.class.getName());
        timePoint(Instant.class, Function.identity(), Function.identity());
        timePoint(LocalDateTime.class, value -> value.toInstant(ZoneOffset.UTC),
                value -> LocalDateTime.ofInstant(value, ZoneOffset.UTC));
        timePoint(Date.class, Date::toInstant, Date::from);
        timePoint(OffsetDateTime.class, OffsetDateTime::toInstant, value -> value.atOffset(ZoneOffset.UTC));
        timePoint(ZonedDateTime.class, ZonedDateTime::toInstant, value -> value.atZone(ZoneOffset.UTC));
        DateTimeFormatter date = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter time = DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN);
        addSerializer(LocalDate.class, new LocalDateSerializer(date));
        addDeserializer(LocalDate.class, new LocalDateDeserializer(date));
        addSerializer(LocalTime.class, new LocalTimeSerializer(time));
        addDeserializer(LocalTime.class, new LocalTimeDeserializer(time));
    }

    private <T> void timePoint(Class<T> type, Function<T, Instant> toInstant, Function<Instant, T> fromInstant) {
        addSerializer(type, new TimePointSerializer<>(type, toInstant));
        addDeserializer(type, new TimePointDeserializer<>(type, fromInstant));
    }

    /** 时间点输出器，始终保留 UTC 和原始精度。 */
    private static final class TimePointSerializer<T> extends StdScalarSerializer<T> {
        private final Function<T, Instant> toInstant;

        private TimePointSerializer(Class<T> type, Function<T, Instant> toInstant) {
            super(type);
            this.toInstant = toInstant;
        }

        @Override
        public void serialize(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(toInstant.apply(value).toString());
        }
    }

    /** 时间点输入器，拒绝数字、空字符串、无时区和非法日期。 */
    private static final class TimePointDeserializer<T> extends StdScalarDeserializer<T> {
        private final Class<T> type;
        private final Function<Instant, T> fromInstant;

        private TimePointDeserializer(Class<T> type, Function<Instant, T> fromInstant) {
            super(type);
            this.type = type;
            this.fromInstant = fromInstant;
        }

        @Override
        public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return type.cast(context.handleUnexpectedToken(type, parser));
            }
            String value = parser.getText();
            try {
                return fromInstant.apply(ApiTime.parse(value));
            } catch (DateTimeException | IllegalArgumentException exception) {
                return type.cast(context.handleWeirdStringValue(type, value,
                        "时间点必须为带 Z 或偏移量的 ISO-8601: %s", exception.getMessage()));
            }
        }
    }
}
