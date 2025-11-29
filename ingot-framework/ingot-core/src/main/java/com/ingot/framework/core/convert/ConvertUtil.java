package com.ingot.framework.core.convert;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : ConvertUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 17:03.</p>
 */
@Slf4j
public class ConvertUtil {

    @SuppressWarnings("unchecked")
    static <T> T convert(Object source, Method deserializeMethod, Class<T> enumType) {
        if (source == null) return null;
        try {
            if (deserializeMethod != null) {
                return (T) deserializeMethod.invoke(null, source);
            }
            log.warn("Cannot find EnumDeserializeMethod method in {}", enumType);
            return null;
        } catch (Exception e) {
            log.error("无法将 {} 转换为 {}", source, enumType.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 查找被 @EnumDeserializeMethod 或 {@link JsonCreator} 标记的静态方法
     */
    static Method findAnnotatedMethod(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(EnumDeserializeMethod.class)
                        || m.isAnnotationPresent(JsonCreator.class))
                .filter(m -> java.lang.reflect.Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
    }
}
