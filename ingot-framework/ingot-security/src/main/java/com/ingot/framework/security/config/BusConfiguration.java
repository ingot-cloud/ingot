package com.ingot.framework.security.config;

import com.ingot.framework.security.core.bus.RefreshJwtKeySender;
import com.ingot.framework.security.core.bus.event.RefreshJwtKeyApplicationEvent;
import com.ingot.framework.security.core.bus.listener.RefreshJwtKeyEventListener;
import com.ingot.framework.security.service.JwtKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

/**
 * <p>Description  : BusConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-25.</p>
 * <p>Time         : 11:16.</p>
 */
@Slf4j
@Configuration
@ConditionalOnBusEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@RemoteApplicationEventScan(basePackageClasses = RefreshJwtKeyApplicationEvent.class)
public class BusConfiguration {

    @Bean
    public RefreshJwtKeySender refreshJwtKeySender(BusProperties properties,
                                                   ApplicationEventPublisher publisher){
        return new RefreshJwtKeySender(properties.getId(), publisher);
    }


    @Bean
    @ConditionalOnBean(JwtAccessTokenConverter.class)
    @ConditionalOnProperty(name = "security.oauth2.resource.jwt.key-uri")
    public RefreshJwtKeyEventListener refreshJwtKeyEventListener(JwtAccessTokenConverter jwtTokenEnhancer,
                                                                 JwtKeyService jwtKeyService){
        return new RefreshJwtKeyEventListener(jwtTokenEnhancer, jwtKeyService);
    }
}
