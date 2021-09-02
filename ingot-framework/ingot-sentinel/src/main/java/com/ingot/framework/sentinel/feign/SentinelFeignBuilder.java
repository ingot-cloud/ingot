package com.ingot.framework.sentinel.feign;

import com.alibaba.cloud.sentinel.feign.SentinelContractHolder;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Target;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * <p>Description  : Builder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 4:26 下午.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SentinelFeignBuilder extends Feign.Builder implements ApplicationContextAware {

    private Contract contract = new Contract.Default();

    private ApplicationContext applicationContext;

    private FeignContext feignContext;

    @Override
    public Feign.Builder invocationHandlerFactory(
            InvocationHandlerFactory invocationHandlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SentinelFeignBuilder contract(Contract contract) {
        this.contract = contract;
        return this;
    }

    @Override
    public Feign build() {
        super.invocationHandlerFactory(new InvocationHandlerFactory() {
            @Override
            public InvocationHandler create(Target target,
                                            Map<Method, MethodHandler> dispatch) {

                GenericApplicationContext gctx = (GenericApplicationContext) SentinelFeignBuilder.this.applicationContext;
                BeanDefinition def = gctx.getBeanDefinition(target.type().getName());

                /**
                 * Due to the change of the initialization sequence, BeanFactory.getBean will cause a circular dependency.
                 * So FeignClientFactoryBean can only be obtained from BeanDefinition
                 */
                FeignClientFactoryBean feignClientFactoryBean = (FeignClientFactoryBean) def.getAttribute("feignClientsRegistrarFactoryBean");

                Class fallback = feignClientFactoryBean.getFallback();
                Class fallbackFactory = feignClientFactoryBean.getFallbackFactory();
                String beanName = feignClientFactoryBean.getContextId();

                if (!StringUtils.hasText(beanName)) {
                    beanName = feignClientFactoryBean.getName();
                }

                Object fallbackInstance;
                FallbackFactory fallbackFactoryInstance;
                // check fallback and fallbackFactory properties
                if (void.class != fallback) {
                    fallbackInstance = getFromContext(beanName, "fallback", fallback,
                            target.type());
                    return new IngotSentinelInvocationHandler(target, dispatch,
                            new FallbackFactory.Default(fallbackInstance));
                }
                if (void.class != fallbackFactory) {
                    fallbackFactoryInstance = (FallbackFactory) getFromContext(
                            beanName, "fallbackFactory", fallbackFactory,
                            FallbackFactory.class);
                    return new IngotSentinelInvocationHandler(target, dispatch,
                            fallbackFactoryInstance);
                }
                return new IngotSentinelInvocationHandler(target, dispatch);
            }

            private Object getFromContext(String name, String type,
                                          Class fallbackType, Class targetType) {
                Object fallbackInstance = feignContext.getInstance(name,
                        fallbackType);
                if (fallbackInstance == null) {
                    throw new IllegalStateException(String.format(
                            "No %s instance of type %s found for feign client %s",
                            type, fallbackType, name));
                }

                if (!targetType.isAssignableFrom(fallbackType)) {
                    throw new IllegalStateException(String.format(
                            "Incompatible %s instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
                            type, fallbackType, targetType, name));
                }
                return fallbackInstance;
            }
        });

        super.contract(new SentinelContractHolder(contract));
        return super.build();
    }

    private Object getFieldValue(Object instance, String fieldName) {
        Field field = ReflectionUtils.findField(instance.getClass(), fieldName);
        assert field != null;
        field.setAccessible(true);
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            // ignore
        }
        return null;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
        feignContext = this.applicationContext.getBean(FeignContext.class);
    }

}