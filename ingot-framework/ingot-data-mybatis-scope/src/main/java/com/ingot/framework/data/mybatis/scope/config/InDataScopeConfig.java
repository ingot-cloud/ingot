package com.ingot.framework.data.mybatis.scope.config;

import com.ingot.cloud.pms.api.rpc.PmsDataScopeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * <p>Description  : InDataScopeConfig.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/14.</p>
 * <p>Time         : 15:46.</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(DataScopeProperties.class)
public class InDataScopeConfig {

    @Bean
    public DataScopeTableResolver dataScopeTableResolver(DataScopeProperties properties) {
        return new DataScopeTableResolver(properties);
    }

    @Bean
    public DataScopeAOP dataScopeAOP(PmsDataScopeService pmsDataScopeService) {
        return new DataScopeAOP(pmsDataScopeService);
    }
}
