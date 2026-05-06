package com.ingot.cloud.pms.service.dict;

import com.ingot.cloud.pms.service.biz.BizPlatformDictService;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.config.DictClientAutoConfiguration;
import com.ingot.framework.dict.client.config.DictClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PMS 自身进程内的字典实现装配。仅注册 L0 delegate（{@link LocalDictService}）作为 {@code dictDelegate} bean。
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
public class LocalDictConfig {

    @Bean(name = DictClientAutoConfiguration.DICT_DELEGATE_SERVICE_NAME)
    public DictService dictDelegate(BizPlatformDictService bizDictService) {
        log.info("[DictClient] register local delegate (LocalDictService)");
        return new LocalDictService(bizDictService);
    }
}
