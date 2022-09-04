package com.ingot.framework.security.config;

import com.ingot.framework.security.provider.IngotJwtTokenServicesBeanDefinitionRegistrar;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * <p>Description  : JwtTokenConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/24.</p>
 * <p>Time         : 2:40 PM.</p>
 */
@Slf4j
@Configuration
@Import({IngotJwtTokenServicesBeanDefinitionRegistrar.class})
@ConditionalOnProperty(name = {
        "security.oauth2.resource.jwt.key-uri"
})
@RequiredArgsConstructor
public class IngotTokenServicesConfiguration {

}
