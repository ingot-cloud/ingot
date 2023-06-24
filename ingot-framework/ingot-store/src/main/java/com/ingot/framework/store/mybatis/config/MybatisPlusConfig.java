package com.ingot.framework.store.mybatis.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ingot.framework.store.mybatis.filter.DruidSqlLogFilter;
import com.ingot.framework.store.mybatis.plugins.IngotOptimisticLockerInterceptor;
import com.ingot.framework.store.mybatis.plugins.IngotTenantLineHandler;
import com.ingot.framework.store.mybatis.properties.MybatisProperties;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * <p>Description  : MybatisPlusConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 5:10 下午.</p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@MapperScan("com.ingot.**.mapper")
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantProperties tenantProperties) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();

        // tenant
        TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
        tenantLineInnerInterceptor.setTenantLineHandler(new IngotTenantLineHandler(tenantProperties));
        mybatisPlusInterceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        // version
        mybatisPlusInterceptor.addInnerInterceptor(new IngotOptimisticLockerInterceptor());
        // page
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }

    @Bean
    public DruidSqlLogFilter sqlLogFilter(MybatisProperties properties) {
        if (properties.isShowSqlLog()) {
            log.warn("\n\n" +
                    "\n********************************************************************" +
                    "\n**********                SQL日志打印已经打开               **********" +
                    "\n**********             包含敏感信息请不要用于生产            **********" +
                    "\n********************************************************************" +
                    "\n\n");
        }
        return new DruidSqlLogFilter(properties);
    }
}
