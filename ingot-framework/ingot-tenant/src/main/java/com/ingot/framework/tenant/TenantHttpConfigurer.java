package com.ingot.framework.tenant;

import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurer;
import com.ingot.framework.tenant.filter.TenantFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.HeaderWriterFilter;

/**
 * <p>把 {@link TenantFilter} 挂到 Security 过滤器链中。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Slf4j
public class TenantHttpConfigurer extends InHttpConfigurer {

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        log.info("[TenantHttpConfigurer] Config TenantFilter.");
        builder.addFilterAfter(new TenantFilter(), HeaderWriterFilter.class);
    }
}
