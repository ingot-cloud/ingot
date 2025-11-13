package com.ingot.framework.core.convert;

import java.lang.reflect.Method;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

/**
 * <p>Description  : IntegerToEnumConverterFactory.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 15:02.</p>
 */
public class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum<?>> {

    @Override
    @NonNull
    public <T extends Enum<?>> Converter<Integer, T> getConverter(@NonNull Class<T> targetType) {
        return new IntegerToEnumConverter<>(targetType);
    }

    private static class IntegerToEnumConverter<T extends Enum<?>> implements Converter<Integer, T> {
        private final Class<T> enumType;
        private final Method deserializeMethod;

        public IntegerToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
            this.deserializeMethod = ConvertUtil.findAnnotatedMethod(enumType);
        }

        @Override
        public T convert(Integer source) {
            return ConvertUtil.convert(source, deserializeMethod, enumType);
        }

    }
}
