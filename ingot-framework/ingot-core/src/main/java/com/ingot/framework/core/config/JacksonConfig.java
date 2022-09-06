package com.ingot.framework.core.config;

import java.util.Locale;
import java.util.TimeZone;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.jackson.IngotLocalTimeModule;
import com.ingot.framework.core.jackson.IngotModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : JacksonConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/17.</p>
 * <p>Time         : 8:37 下午.</p>
 */
@AutoConfiguration
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

    private static final String ASIA_BEIJING = "Asia/Beijing";

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.locale(Locale.CHINA);
            builder.timeZone(TimeZone.getTimeZone(ASIA_BEIJING));
            builder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
            builder.modules(new IngotLocalTimeModule(), new IngotModule());
        };
    }
}
