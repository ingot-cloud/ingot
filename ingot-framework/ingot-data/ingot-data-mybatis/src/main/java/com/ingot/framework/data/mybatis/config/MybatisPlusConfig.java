package com.ingot.framework.data.mybatis.config;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ingot.framework.data.mybatis.plugins.CustomDataPermissionHandler;
import com.ingot.framework.data.mybatis.plugins.DruidSqlLogFilter;
import com.ingot.framework.data.mybatis.plugins.InOptimisticLockerInterceptor;
import com.ingot.framework.data.mybatis.plugins.InTenantLineHandler;
import com.ingot.framework.data.mybatis.properties.MybatisProperties;
import com.ingot.framework.data.mybatis.scope.config.DataScopeProperties;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : MybatisPlusConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 5:10 下午.</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantProperties tenantProperties,
                                                         DataScopeProperties dataScopeProperties) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();

        // tenant
        TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
        tenantLineInnerInterceptor.setTenantLineHandler(new InTenantLineHandler(tenantProperties));
        mybatisPlusInterceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        // version
        mybatisPlusInterceptor.addInnerInterceptor(new InOptimisticLockerInterceptor());
        // page
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // permission
        CustomDataPermissionHandler permissionHandler = new CustomDataPermissionHandler(dataScopeProperties);
        DataPermissionInterceptor permissionInterceptor = new DataPermissionInterceptor();
        permissionInterceptor.setDataPermissionHandler(permissionHandler);
        mybatisPlusInterceptor.addInnerInterceptor(permissionInterceptor);
        return mybatisPlusInterceptor;
    }

    @Bean
    public DruidSqlLogFilter sqlLogFilter(MybatisProperties properties) {
        if (properties.isShowSqlLog()) {
            log.warn("""
                    
                    
                    ********************************************************************\
                    
                    **********                SQL日志打印已经打开              ************\
                    
                    **********             包含敏感信息请不要用于生产            ************\
                    
                    ********************************************************************\
                    
                    
                    """);
        }
        return new DruidSqlLogFilter(properties);
    }

    @Bean
    public TenantResolver tenantResolver(TenantProperties properties) {
        return new TenantResolver(properties);
    }
}
