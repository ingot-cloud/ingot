package com.ingot.framework.core.config;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.core.jackson.InJavaTimeModule;
import com.ingot.framework.core.jackson.InModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>Description  : JacksonConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/17.</p>
 * <p>Time         : 8:37 下午.</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

    private static final String ASIA_BEIJING = "Asia/Beijing";

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.locale(Locale.getDefault());
            builder.timeZone(TimeZone.getTimeZone(ASIA_BEIJING));
            builder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
            // IngotJavaTimeModule 覆盖 JavaTimeModule 中部分Class Type
            builder.modules(new InModule(), new JavaTimeModule(), new InJavaTimeModule());
            builder.failOnUnknownProperties(false);
        };
    }
}
