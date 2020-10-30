package com.ingot.framework.security.provider;

import com.ingot.framework.security.provider.token.store.IngotJwtAccessTokenConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

/**
 * <p>Description  : IngotJwtTokenServicesBeanDefinitionRegistrar.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 13:56.</p>
 */
@Slf4j
public class IngotJwtTokenServicesBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                                  BeanDefinitionRegistry registry) {
        // 使用 ingotJwtTokenEnhancer 替换
        // org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration
        // 类中的 jwtTokenEnhancer
        if (registry.isBeanNameInUse("jwtTokenEnhancer")){
            log.info(">>> IngotJwtTokenServicesBeanDefinitionRegistrar - jwtTokenEnhancer 使用 IngotJwtAccessTokenConverter 进行注册。");
            registry.removeBeanDefinition("jwtTokenEnhancer");

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(IngotJwtAccessTokenConverter.class);
            registry.registerBeanDefinition("jwtTokenEnhancer", beanDefinition);
        } else {
            log.info(">>> IngotJwtTokenServicesBeanDefinitionRegistrar - 注册表中没有使用 jwtTokenEnhancer 作为名称的bean。");
        }
    }
}
