package com.ingot.framework.feign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

/**
 * WebFlux 环境下 Feign 所需的 {@link HttpMessageConverters} 装配验证。
 */
class FeignReactiveHttpMessageConvertersTest {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class));

    @Test
    void registersHttpMessageConvertersWithFallbackWhenNoJacksonAutoConfig() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HttpMessageConverters.class);
            assertThat(context.getBean(HttpMessageConverters.class).getConverters()).isNotEmpty();
        });
    }

    @Test
    void reusesExistingHttpMessageConverterBeans() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpMessageConverters.class);
                    assertThat(context.getBean(HttpMessageConverters.class).getConverters()).isNotEmpty();
                });
    }
}
