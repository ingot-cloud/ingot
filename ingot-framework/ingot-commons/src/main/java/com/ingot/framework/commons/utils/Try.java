package com.ingot.framework.commons.utils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.Getter;

/**
 * <p>Description  :
 * 轻量版 Try 工具类（模仿 Vavr）
 * 支持 Supplier、Function、Consumer、Predicate 的异常包装。
 * 示例：
 * <pre>
 * list.stream()
 *     .map(Try.lift(this::dangerous))
 *     .map(t -> t.getOrElse("fallback"))
 *     .toList();
 * </pre>
 * </p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/6.</p>
 * <p>Time         : 09:43.</p>
 */
public final class Try<T> {

    private final T value;
    @Getter
    private final Throwable exception;

    private Try(T value, Throwable exception) {
        this.value = value;
        this.exception = exception;
    }

    /** 运行带异常的 Supplier */
    public static <T> Try<T> of(ThrowingSupplier<T> supplier) {
        Objects.requireNonNull(supplier);
        try {
            return success(supplier.get());
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /** 成功结果 */
    public static <T> Try<T> success(T value) {
        return new Try<>(value, null);
    }

    /** 失败结果 */
    public static <T> Try<T> failure(Throwable e) {
        return new Try<>(null, e);
    }

    /** 是否成功 */
    public boolean isSuccess() {
        return exception == null;
    }

    /** 是否失败 */
    public boolean isFailure() {
        return exception != null;
    }

    /** 获取结果，失败时抛 RuntimeException */
    public T get() {
        if (isFailure()) {
            if (exception instanceof RuntimeException re) throw re;
            throw new RuntimeException(exception);
        }
        return value;
    }

    /** 获取结果或默认值 */
    public T getOrElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    /** 获取结果或使用 Supplier 提供默认值 */
    public T getOrElseGet(Supplier<T> supplier) {
        return isSuccess() ? value : supplier.get();
    }

    /** 自定义异常映射并抛出 */
    public <X extends Throwable> T getOrThrow(Function<Throwable, X> mapper) throws X {
        if (isSuccess()) return value;
        throw mapper.apply(exception);
    }

    /** 成功回调 */
    public Try<T> onSuccess(Consumer<T> action) {
        if (isSuccess()) action.accept(value);
        return this;
    }

    /** 失败回调 */
    public Try<T> onFailure(Consumer<Throwable> action) {
        if (isFailure()) action.accept(exception);
        return this;
    }

    /** 映射成功结果 */
    public <U> Try<U> map(Function<T, U> mapper) {
        if (isFailure()) return failure(exception);
        try {
            return success(mapper.apply(value));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /** 平铺映射 */
    public <U> Try<U> flatMap(Function<T, Try<U>> mapper) {
        if (isFailure()) return failure(exception);
        try {
            return Objects.requireNonNull(mapper.apply(value));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /** 异常恢复逻辑 */
    public Try<T> recover(Function<Throwable, T> recoverFn) {
        if (isSuccess()) return this;
        try {
            return success(recoverFn.apply(exception));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /* -------------------  Lift 工具方法 ------------------- */

    /** 将可抛异常的 Supplier 包装成 Try */
    public static <T> Supplier<Try<T>> lift(ThrowingSupplier<T> supplier) {
        return () -> Try.of(supplier);
    }

    /** 将可抛异常的 Function 包装成 Try */
    public static <T, R> Function<T, Try<R>> lift(ThrowingFunction<T, R> func) {
        return t -> Try.of(() -> func.apply(t));
    }

    /** 将可抛异常的 Consumer 包装成 Try */
    public static <T> Consumer<T> liftConsumer(ThrowingConsumer<T> consumer) {
        return t -> Try.of(() -> {
            consumer.accept(t);
            return null;
        });
    }

    /** 将可抛异常的 Predicate 包装成 Try<Boolean> */
    public static <T> Function<T, Try<Boolean>> liftPredicate(ThrowingPredicate<T> predicate) {
        return t -> Try.of(() -> predicate.test(t));
    }

    /* ------------------- 函数式接口定义 ------------------- */

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingPredicate<T> {
        boolean test(T t) throws Exception;
    }
}