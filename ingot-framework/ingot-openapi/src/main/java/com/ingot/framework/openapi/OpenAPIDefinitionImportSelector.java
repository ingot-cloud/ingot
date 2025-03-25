package com.ingot.framework.openapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * <p>Description  : OpenAPIDefinitionImportSelector.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/20.</p>
 * <p>Time         : 09:51.</p>
 */
@Slf4j
public class OpenAPIDefinitionImportSelector implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(EnableOpenAPI.class.getName(),
                true);
        if (annotationAttributes == null) {
            return;
        }
        Object value = annotationAttributes.get("value");
        if (Objects.isNull(value)) {
            return;
        }

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(OpenAPIDefinition.class);
        definition.addPropertyValue("path", value);
        definition.setPrimary(true);

        registry.registerBeanDefinition("openAPIDefinition", definition.getBeanDefinition());

        // 如果是微服务架构则，引入了服务发现声明相关的元数据配置
        Object isMicro = annotationAttributes.getOrDefault("isMicro", true);
        if (isMicro.equals(false)) {
            return;
        }

        BeanDefinitionBuilder openAPIMetadata = BeanDefinitionBuilder
                .genericBeanDefinition(OpenAPIMetadataConfiguration.class);
        openAPIMetadata.addPropertyValue("path", value);
        registry.registerBeanDefinition("openAPIMetadata", openAPIMetadata.getBeanDefinition());
    }

}
