package com.ingot.framework.tenant;

import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurer;
import com.ingot.framework.tenant.filter.TenantFilter;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class TenantHttpConfigurer extends InHttpConfigurer {
    private final TenantProperties tenantProperties;

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        log.info("[TenantHttpConfigurer] Config TenantFilter.");
        TenantFilter filter = new TenantFilter(tenantProperties);
        builder.addFilterAfter(filter, HeaderWriterFilter.class);
    }
}
