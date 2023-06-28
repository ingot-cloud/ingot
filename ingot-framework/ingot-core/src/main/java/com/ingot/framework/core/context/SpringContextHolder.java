package com.ingot.framework.core.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * <p>Description  : SpringContextHolder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 12:15 PM.</p>
 */
@Slf4j
public final class SpringContextHolder implements BeanFactoryPostProcessor, ApplicationContextAware, DisposableBean {

    private static ConfigurableListableBeanFactory beanFactory;

    private static ApplicationContext applicationContext;

    /**
     * 获取 {@link ApplicationContext}
     *
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取BeanFactory
     *
     * @return {@link ListableBeanFactory}
     */
    public static ListableBeanFactory getBeanFactory() {
        return beanFactory == null ? applicationContext : beanFactory;
    }

    /**
     * 根据名字获取Bean
     *
     * @param name 名称
     * @param <T>  Bean类型
     * @return 强转后的Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) getBeanFactory().getBean(name);
    }

    /**
     * 获取指定类型的Bean
     *
     * @param requiredType Class
     * @param <T>          类型
     * @return Bean
     */
    public static <T> T getBean(Class<T> requiredType) {
        return getBeanFactory().getBean(requiredType);
    }

    /**
     * 获取指定类型的所有Bean
     *
     * @param type Class
     * @param <T>  类型
     * @return Bean Map
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return getBeanFactory().getBeansOfType(type);
    }

    /**
     * 发布事件
     *
     * @param event {@link ApplicationEvent}
     */
    public static void publishEvent(ApplicationEvent event) {
        if (applicationContext == null) {
            return;
        }
        applicationContext.publishEvent(event);
    }

    /**
     * 发布事件
     *
     * @param event object
     */
    public static void publishEvent(Object event) {
        if (applicationContext == null) {
            return;
        }
        applicationContext.publishEvent(event);
    }

    /**
     * 清空Holder
     */
    public static void clearHolder() {
        applicationContext = null;
        beanFactory = null;
    }

    @Override
    public void destroy() throws Exception {
        SpringContextHolder.clearHolder();
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringContextHolder.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }
}
