package com.ingot.framework.core.config;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.core.jackson.InJackson2ObjectMapperBuilderCustomizer;
import com.ingot.framework.core.jackson.InJavaTimeModule;
import com.ingot.framework.core.jackson.InModule;
import com.ingot.framework.core.jackson.InJacksonModule;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

    private static final String ASIA_BEIJING = "Asia/Beijing";

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer customizer(List<InJackson2ObjectMapperBuilderCustomizer> customizers,
                                                            List<InJacksonModule> modules) {
        return builder -> {
            builder.locale(Locale.getDefault());
            builder.timeZone(TimeZone.getTimeZone(ASIA_BEIJING));
            builder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
            // IngotJavaTimeModule 覆盖 JavaTimeModule 中部分Class Type
            builder.modules((list) -> {
                list.add(new InModule());
                list.add(new JavaTimeModule());
                list.add(new InJavaTimeModule());
                if (CollUtil.isNotEmpty(modules)) {
                    list.addAll(modules.stream()
                            .sorted(Comparator.comparingInt(InJacksonModule::getOrder))
                            .toList());
                }
            });
            builder.failOnUnknownProperties(false);

            // ext
            customizers.forEach(customizer -> customizer.customize(builder));
        };
    }
}
