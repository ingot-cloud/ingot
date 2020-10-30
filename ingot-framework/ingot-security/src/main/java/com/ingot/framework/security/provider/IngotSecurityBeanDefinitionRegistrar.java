package com.ingot.framework.security.provider;

import com.ingot.framework.core.constants.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

/**
 * <p>Description  : IngotSecurityBeanDefinitionRegistrar.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 13:37.</p>
 */
@Slf4j
public class IngotSecurityBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                                  BeanDefinitionRegistry registry) {
        if (registry.isBeanNameInUse(SecurityConstants.RESOURCE_SERVER_CONFIGURER)) {
            log.warn(">>> 本地存在资源服务器配置={}, 不注入默认资源配置 IngotResourceServerConfig",
                    SecurityConstants.RESOURCE_SERVER_CONFIGURER);
            return;
        }

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(IngotResourceServerConfig.class);
        registry.registerBeanDefinition(SecurityConstants.RESOURCE_SERVER_CONFIGURER, beanDefinition);
    }
}
