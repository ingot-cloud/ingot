package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : AbstractOAuth2Configurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 9:50 AM.</p>
 */
abstract class AbstractOAuth2Configurer {

    private final ObjectPostProcessor<Object> objectPostProcessor;

    AbstractOAuth2Configurer(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
    }

    abstract void init(HttpSecurity httpSecurity);

    abstract void configure(HttpSecurity httpSecurity);

    abstract RequestMatcher getRequestMatcher();

    protected final <T> T postProcess(T object) {
        return (T) this.objectPostProcessor.postProcess(object);
    }

    protected final ObjectPostProcessor<Object> getObjectPostProcessor() {
        return this.objectPostProcessor;
    }
}
