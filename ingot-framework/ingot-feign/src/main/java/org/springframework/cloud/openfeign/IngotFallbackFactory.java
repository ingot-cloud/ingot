package org.springframework.cloud.openfeign;

import feign.Target;
import feign.hystrix.FallbackFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;

/**
 * <p>Description  : IngotFallbackFactory. 默认 Fallback，避免写过多fallback类.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/26.</p>
 * <p>Time         : 3:45 PM.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotFallbackFactory<T> implements FallbackFactory<T> {
    private final Target<T> target;

    @SuppressWarnings("unchecked")
    @Override public T create(Throwable cause) {
        final Class<T> targetType = target.type();
        final String targetName = target.name();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetType);
        enhancer.setUseCache(true);
        enhancer.setCallback(new IngotFeignFallback<>(targetType, targetName, cause));
        return (T) enhancer.create();
    }
}
