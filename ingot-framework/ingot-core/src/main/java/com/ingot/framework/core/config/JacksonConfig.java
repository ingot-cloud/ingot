package com.ingot.framework.core.config;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.commons.jackson.ClientWallClock;
import com.ingot.framework.commons.jackson.InJackson2ObjectMapperBuilderCustomizer;
import com.ingot.framework.commons.jackson.InJacksonModule;
import com.ingot.framework.commons.jackson.InJavaTimeModule;
import com.ingot.framework.commons.jackson.InModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * <p>注册接口 JSON 基础模块和默认时区，并叠加业务模块及定制器。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

    /**
     * 基础配置与业务定制器共同生效，业务扩展不能抑制框架时间及长整型契约。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer(List<InJackson2ObjectMapperBuilderCustomizer> customizers,
                                                            List<InJacksonModule> modules) {
        return builder -> {
            builder.locale(Locale.getDefault());
            builder.timeZone(TimeZone.getTimeZone(ClientWallClock.FALLBACK_ZONE));
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
