package com.ingot.framework.tenant;

import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurer;
import com.ingot.framework.tenant.filter.TenantFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.HeaderWriterFilter;

/**
 * <p>Description  : TenantFilterConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/25.</p>
 * <p>Time         : 4:13 下午.</p>
 */
@Slf4j
public class TenantHttpConfigurer extends IngotHttpConfigurer {

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        log.info("Config TenantFilter.");
        TenantFilter filter = new TenantFilter();
        builder.addFilterAfter(filter, HeaderWriterFilter.class);
    }
}