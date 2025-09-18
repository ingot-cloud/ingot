package com.ingot.framework.security.config.annotation.web.configurers;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * <p>Description  : DefaultHttpConfigurersAdapter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 5:38 下午.</p>
 */
@RequiredArgsConstructor
public class DefaultHttpConfigurersAdapter implements InHttpConfigurersAdapter {
    private final List<InHttpConfigurer> httpConfigurers;

    @Override
    public void apply(HttpSecurity http) throws Exception {
        for (InHttpConfigurer configurer : this.httpConfigurers) {
            http.with(configurer, Customizer.withDefaults());
        }
    }
}
