package com.ingot.framework.security.provider;

import com.ingot.framework.security.provider.token.store.IngotJwtAccessTokenConverter;
import com.ingot.framework.security.provider.token.store.IngotJwtTokenStore;
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
    private static final String BEAN_NAME_JWT_TOKEN_STORE = "jwtTokenStore";
    private static final String BEAN_NAME_JWT_TOKEN_ENHANCER = "jwtTokenEnhancer";

    @Override public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                                  BeanDefinitionRegistry registry) {
        // 使用 ingotJwtTokenEnhancer 替换
        // org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration
        // 类中的 jwtTokenEnhancer
        if (registry.isBeanNameInUse(BEAN_NAME_JWT_TOKEN_ENHANCER)){
            log.info(">>> IngotJwtTokenServicesBeanDefinitionRegistrar - {} 使用 IngotJwtAccessTokenConverter 进行注册。",
                    BEAN_NAME_JWT_TOKEN_ENHANCER);
            registry.removeBeanDefinition(BEAN_NAME_JWT_TOKEN_ENHANCER);

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(IngotJwtAccessTokenConverter.class);
            registry.registerBeanDefinition(BEAN_NAME_JWT_TOKEN_ENHANCER, beanDefinition);
        } else {
            throw new RuntimeException(">>> IngotJwtTokenServicesBeanDefinitionRegistrar - 注册表中没有使用 "
                    + BEAN_NAME_JWT_TOKEN_ENHANCER + " 作为名称的bean。");
        }

        // 替换 jwtTokenStore
        if (registry.isBeanNameInUse(BEAN_NAME_JWT_TOKEN_STORE)){
            log.info(">>> IngotJwtTokenServicesBeanDefinitionRegistrar - jwtTokenEnhancer 使用 IngotJwtAccessTokenConverter 进行注册。");
            registry.removeBeanDefinition(BEAN_NAME_JWT_TOKEN_STORE);

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(IngotJwtTokenStore.class);
            registry.registerBeanDefinition(BEAN_NAME_JWT_TOKEN_STORE, beanDefinition);
        } else {
            throw new RuntimeException(">>> IngotJwtTokenServicesBeanDefinitionRegistrar - 注册表中没有使用 "
                    + BEAN_NAME_JWT_TOKEN_STORE + " 作为名称的bean。");
        }
    }
}
