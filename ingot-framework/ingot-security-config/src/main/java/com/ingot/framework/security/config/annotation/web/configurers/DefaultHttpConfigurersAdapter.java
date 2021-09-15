package com.ingot.framework.security.config.annotation.web.configurers;

import lombok.AllArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

/**
 * <p>Description  : DefaultHttpConfigurersAdapter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 5:38 下午.</p>
 */
@AllArgsConstructor
public class DefaultHttpConfigurersAdapter implements IngotHttpConfigurersAdapter{
    private final List<IngotHttpConfigurer> httpConfigurers;

    @Override
    public void apply(HttpSecurity http) throws Exception {
        for (IngotHttpConfigurer configurer : this.httpConfigurers) {
            http.apply(configurer);
        }
    }
}
