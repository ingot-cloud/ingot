package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.config.annotation.web.configurers.DefaultHttpConfigurersAdapter;
import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurer;
import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurersAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;

/**
 * <p>Description  : SecurityAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 5:32 下午.</p>
 */
public class SecurityAutoConfiguration {
    private List<IngotHttpConfigurer> httpConfigurers = Collections.emptyList();

    @Bean
    @ConditionalOnMissingBean(IngotHttpConfigurersAdapter.class)
    public IngotHttpConfigurersAdapter ingotHttpConfigurersAdapter() {
        return new DefaultHttpConfigurersAdapter(this.httpConfigurers);
    }

    @Autowired(required = false)
    public void setHttpConfigurers(List<IngotHttpConfigurer> httpConfigurers) {
        this.httpConfigurers = httpConfigurers;
    }
}
