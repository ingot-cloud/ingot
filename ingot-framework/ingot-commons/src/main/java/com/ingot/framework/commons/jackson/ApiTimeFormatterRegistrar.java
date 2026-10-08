package com.ingot.framework.commons.jackson;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;

/**
 * <p>MVC/Feign 参数统一使用带偏移量的 ISO 时间点；UTC LocalDateTime 输出时补充 Z。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ApiTimeFormatterRegistrar implements FormatterRegistrar {
    /** 注册时间点参数的双向转换，不覆盖日期及日内时间格式。 */
    @Override
    public void registerFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Instant.class, ApiTime::parse);
        registry.addConverter(String.class, LocalDateTime.class, ApiTime::parseUtc);
        registry.addConverter(String.class, Date.class, value -> Date.from(ApiTime.parse(value)));
        registry.addConverter(String.class, OffsetDateTime.class, value -> ApiTime.parse(value).atOffset(ZoneOffset.UTC));
        registry.addConverter(String.class, ZonedDateTime.class, value -> ApiTime.parse(value).atZone(ZoneOffset.UTC));
        registry.addConverter(Instant.class, String.class, Instant::toString);
        registry.addConverter(LocalDateTime.class, String.class, value -> value.toInstant(ZoneOffset.UTC).toString());
        registry.addConverter(Date.class, String.class, value -> value.toInstant().toString());
        registry.addConverter(OffsetDateTime.class, String.class, value -> value.toInstant().toString());
        registry.addConverter(ZonedDateTime.class, String.class, value -> value.toInstant().toString());
    }
}
