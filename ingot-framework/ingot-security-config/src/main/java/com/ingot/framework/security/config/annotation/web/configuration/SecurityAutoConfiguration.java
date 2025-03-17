package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.config.annotation.web.configurers.DefaultHttpConfigurersAdapter;
import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurer;
import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurersAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;

/**
 * <p>Description  : SecurityAutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 5:32 下午.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityAutoConfiguration {
    private List<InHttpConfigurer> httpConfigurers = Collections.emptyList();

    @Bean
    @ConditionalOnMissingBean(InHttpConfigurersAdapter.class)
    public InHttpConfigurersAdapter ingotHttpConfigurersAdapter() {
        return new DefaultHttpConfigurersAdapter(this.httpConfigurers);
    }

    @Autowired(required = false)
    public void setHttpConfigurers(List<InHttpConfigurer> httpConfigurers) {
        this.httpConfigurers = httpConfigurers;
    }
}
