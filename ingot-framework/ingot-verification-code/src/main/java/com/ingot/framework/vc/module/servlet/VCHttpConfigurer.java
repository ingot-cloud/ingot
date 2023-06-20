package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.HeaderWriterFilter;

/**
 * <p>Description  : VCHttpConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/29.</p>
 * <p>Time         : 5:27 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VCHttpConfigurer extends IngotHttpConfigurer {
    private final VCProviderManager providerManager;
    private final VCVerifyResolver verifyResolver;
    private final VCFailureHandler failureHandler;

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        log.info("[VCHttpConfigurer] Config TenantFilter.");
        VCFilter filter = new VCFilter(providerManager, verifyResolver, failureHandler);
        builder.addFilterAfter(filter, HeaderWriterFilter.class);
    }

}
