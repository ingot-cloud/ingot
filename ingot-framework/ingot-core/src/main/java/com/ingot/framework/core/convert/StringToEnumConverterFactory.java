package com.ingot.framework.core.convert;

import java.lang.reflect.Method;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

/**
 * <p>Description  : StringToEnumConverterFactory.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 14:39.</p>
 */
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    @Override
    @NonNull
    public <T extends Enum<?>> Converter<String, T> getConverter(@NonNull Class<T> targetType) {
        return new StringToEnumConverter<>(targetType);
    }

    private static class StringToEnumConverter<T extends Enum<?>> implements Converter<String, T> {
        private final Class<T> enumType;
        private final Method deserializeMethod;

        public StringToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
            this.deserializeMethod = ConvertUtil.findAnnotatedMethod(enumType);
        }

        @Override
        public T convert(String source) {
            return ConvertUtil.convert(source, deserializeMethod, enumType);
        }
    }
}
