package com.ingot.framework.commons.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * <p>Description  : YamlPropertySourceFactory.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/18.</p>
 * <p>Time         : 12:49.</p>
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    @NonNull
    public PropertySource<?> createPropertySource(@Nullable String name,
                                                  @NonNull EncodedResource resource) throws IOException {
        Properties propertiesFromYaml = loadYaml(resource);
        String sourceName = name != null ? name : resource.getResource().getFilename();
        assert sourceName != null;
        return new PropertiesPropertySource(sourceName, propertiesFromYaml);
    }

    private Properties loadYaml(EncodedResource resource) throws FileNotFoundException {
        try {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource.getResource());
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause();
            if (cause instanceof FileNotFoundException)
                throw (FileNotFoundException) e.getCause();
            throw e;
        }
    }
}
