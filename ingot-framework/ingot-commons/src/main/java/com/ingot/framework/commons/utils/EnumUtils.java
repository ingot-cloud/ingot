package com.ingot.framework.commons.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>按稳定字面量索引枚举，避免每个枚举复制 HashMap 循环。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class EnumUtils {
    private EnumUtils() {
    }

    /**
     * 按取值函数建立不可变索引，字面量重复时在类加载阶段失败。
     *
     * @param values 枚举全部常量
     * @param value 稳定字面量读取函数
     * @param <E> 枚举类型
     * @return 字面量到枚举的索引
     */
    public static <E extends Enum<E>> Map<String, E> index(E[] values, Function<E, String> value) {
        Map<String, E> index = new LinkedHashMap<>();
        for (E item : values) {
            String key = value.apply(item);
            E existing = index.put(key, item);
            if (existing != null) {
                throw new IllegalStateException("重复枚举值: " + key);
            }
        }
        return Collections.unmodifiableMap(index);
    }

    /**
     * 按稳定字面量查找；{@code null} 或未知值返回 {@code null}。
     *
     * @param index {@link #index(Enum[], Function)} 的结果
     * @param value 稳定字面量，可空
     * @param <E> 枚举类型
     * @return 对应常量；未命中为空
     */
    public static <E extends Enum<E>> E get(Map<String, E> index, String value) {
        return value == null ? null : index.get(value);
    }

    /**
     * 按稳定字面量解析，供 {@code @JsonCreator} 使用。
     *
     * @param index {@link #index(Enum[], Function)} 的结果
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @param <E> 枚举类型
     * @return 对应常量
     * @throws IllegalArgumentException 字面量非空且不在索引中
     */
    public static <E extends Enum<E>> E require(Map<String, E> index, String value) {
        if (value == null) {
            return null;
        }
        E item = index.get(value);
        if (item == null) {
            throw new IllegalArgumentException("未知枚举值: " + value);
        }
        return item;
    }
}
