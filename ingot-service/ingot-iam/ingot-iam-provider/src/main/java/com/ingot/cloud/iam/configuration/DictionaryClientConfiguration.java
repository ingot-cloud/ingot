package com.ingot.cloud.iam.configuration;

import com.ingot.cloud.iam.application.dictionary.LocalDictionaryQueryAdapter;

import com.ingot.cloud.iam.application.dictionary.DictionaryService;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.config.DictClientAutoConfiguration;
import com.ingot.framework.dict.client.config.DictClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IAM 自身进程内的字典实现装配。仅注册 L0 delegate（{@link LocalDictionaryQueryAdapter}）作为 {@code dictDelegate} bean。
 * <p>
 * L1 Caffeine、L2 Redis、{@link DictService} 公共 bean 与 {@code DictCacheCoordinator}
 * 均由 {@code DictClientAutoConfiguration} 统一组合。
 * </p>
 *
 * @author jy
 * @since 2026/4/25
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DictClientProperties.class)
public class DictionaryClientConfiguration {

    @Bean(name = DictClientAutoConfiguration.DICT_DELEGATE_SERVICE_NAME)
    public DictService dictDelegate(DictionaryService bizDictService) {
        log.info("[DictClient] register local delegate (LocalDictionaryQueryAdapter)");
        return new LocalDictionaryQueryAdapter(bizDictService);
    }
}
