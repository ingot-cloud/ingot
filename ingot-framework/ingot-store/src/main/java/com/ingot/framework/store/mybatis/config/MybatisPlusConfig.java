package com.ingot.framework.store.mybatis.config;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.store.mybatis.incrementer.IngotIdGenerator;
import com.ingot.framework.store.mybatis.plugins.IngotOptimisticLockerInterceptor;
import com.ingot.framework.store.mybatis.plugins.IngotTenantLineHandler;
import com.ingot.framework.tenant.properties.TenantProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : MybatisPlusConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 5:10 下午.</p>
 */
@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@MapperScan("com.ingot.**.mapper")
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
    @ConditionalOnBean(IdGenerator.class)
    public IdentifierGenerator idGenerator(IdGenerator idGenerator) {
        return new IngotIdGenerator(idGenerator);
    }
}
