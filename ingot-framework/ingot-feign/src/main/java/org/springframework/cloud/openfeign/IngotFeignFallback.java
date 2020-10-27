package org.springframework.cloud.openfeign;

import cn.hutool.core.io.BufferUtil;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ResponseWrapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * <p>Description  : IngotFeignFallback. 如果不设置 fallback，默认实现</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/26.</p>
 * <p>Time         : 3:45 PM.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotFeignFallback<T> implements MethodInterceptor {
    private final Class<T> targetType;
    private final String targetName;
    private final Throwable cause;

    @Nullable
    @SneakyThrows
    @Override public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) {
        Class<?> returnType = method.getReturnType();
        if (IngotResponse.class != returnType) {
            return null;
        }
        FeignException exception = (FeignException) cause;

        ByteBuffer content = exception.responseBody().orElse(ByteBuffer.allocate(0));
        String str = BufferUtil.readUtf8Str(content);

        log.error(">>> IngotFeignFallback:[{}.{}] serviceId:[{}] message:[{}]", targetType.getName(), method.getName(), targetName, str);
        return ResponseWrapper.error(str);
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IngotFeignFallback<?> that = (IngotFeignFallback<?>) o;
        return targetType.equals(that.targetType);
    }

    @Override public int hashCode() {
        return Objects.hash(targetType);
    }
}