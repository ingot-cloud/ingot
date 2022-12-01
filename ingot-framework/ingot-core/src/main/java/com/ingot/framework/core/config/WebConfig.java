package com.ingot.framework.core.config;

import java.time.format.DateTimeFormatter;

import cn.hutool.core.date.DatePattern;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>Description  : WebConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/1.</p>
 * <p>Time         : 5:10 PM.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebConfig implements WebMvcConfigurer {

    /**
     * <ul>
     * <li>yyyy-MM-dd HH:mm:ss -> LocalDateTime</li>
     * <li>yyyy-MM-dd -> LocalDate</li>
     * <li>HH:mm:ss -> LocalTime</li>
     * </ul>
     */
    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN));
        registrar.setDateFormatter(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN));
        registrar.registerFormatters(registry);
    }
}
