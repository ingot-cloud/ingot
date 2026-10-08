package com.ingot.framework.feign;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>Feign 参数和独立 WebFlux 编解码继承 UTC ISO 契约，不依赖 JVM 时区。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class FeignTimeContractTest {
    @Test
    void queryAndFallbackJsonUseTheSameUtcInstant() {
        TimeZone original = TimeZone.getDefault();
        Instant instant = Instant.parse("2026-10-08T01:00:00.123456789Z");
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                new ReactiveWebApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
                        .run(context -> {
                            assertNull(context.getStartupFailure());
                            var conversion = new DefaultFormattingConversionService();
                            context.getBean(FeignAutoConfiguration.class).apiTimeFeignFormatterRegistrar()
                                    .registerFormatters(conversion);
                            assertEquals(instant.toString(), conversion.convert(
                                    LocalDateTime.ofInstant(instant, ZoneOffset.UTC), String.class));
                            assertEquals(instant, conversion.convert("2026-10-08T09:00:00.123456789+08:00", Instant.class));
                            var mapper = context.getBean(HttpMessageConverters.class).getConverters().stream()
                                    .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                                    .map(MappingJackson2HttpMessageConverter.class::cast)
                                    .findFirst().orElseThrow().getObjectMapper();
                            assertEquals('"' + instant.toString() + '"', mapper.writeValueAsString(instant));
                            assertEquals(instant, mapper.readValue("\"2026-10-08T09:00:00.123456789+08:00\"", Instant.class));
                            assertThrows(Exception.class, () -> mapper.readValue("\"2026-10-08 09:00:00\"", Instant.class));
                        });
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
