package com.ingot.framework.sentinel.feign;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.cloud.sentinel.feign.SentinelContractHolder;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.core.wrapper.ResponseWrapper;
import com.ingot.framework.feign.exception.IngotFeignException;
import feign.Feign;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.MethodMetadata;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import static feign.Util.checkNotNull;

/**
 * <p>Description  : IngotSentinelInvocationHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 4:20 下午.</p>
 */
@Slf4j
@SuppressWarnings({"rawtypes"})
public class IngotSentinelInvocationHandler implements InvocationHandler {

    private final Target<?> target;

    private final Map<Method, MethodHandler> dispatch;

    private FallbackFactory fallbackFactory;

    private Map<Method, Method> fallbackMethodMap;

    IngotSentinelInvocationHandler(Target<?> target, Map<Method, MethodHandler> dispatch,
                                   FallbackFactory fallbackFactory) {
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch");
        this.fallbackFactory = fallbackFactory;
        this.fallbackMethodMap = toFallbackMethod(dispatch);
    }

    IngotSentinelInvocationHandler(Target<?> target, Map<Method, MethodHandler> dispatch) {
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch");
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable {
        switch (method.getName()) {
            case "equals":
                try {
                    Object otherHandler = args.length > 0 && args[0] != null
                            ? Proxy.getInvocationHandler(args[0]) : null;
                    return equals(otherHandler);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            case "hashCode":
                return hashCode();
            case "toString":
                return toString();
        }

        Object result;
        MethodHandler methodHandler = this.dispatch.get(method);
        // only handle by HardCodedTarget
        if (target instanceof Target.HardCodedTarget) {
            Target.HardCodedTarget hardCodedTarget = (Target.HardCodedTarget) target;
            MethodMetadata methodMetadata = SentinelContractHolder.METADATA_MAP
                    .get(hardCodedTarget.type().getName()
                            + Feign.configKey(hardCodedTarget.type(), method));
            // resource default is HttpMethod:protocol://url
            if (methodMetadata == null) {
                result = methodHandler.invoke(args);
            } else {
                String resourceName = methodMetadata.template().method().toUpperCase()
                        + ":" + hardCodedTarget.url() + methodMetadata.template().path();
                Entry entry = null;
                try {
                    ContextUtil.enter(resourceName);
                    entry = SphU.entry(resourceName, EntryType.OUT, 1, args);
                    result = methodHandler.invoke(args);
                } catch (Throwable ex) {
                    // fallback handle
                    if (!BlockException.isBlockException(ex)) {
                        Tracer.trace(ex);
                    }
                    if (fallbackFactory != null) {
                        try {
                            Object fallbackResult = fallbackMethodMap.get(method)
                                    .invoke(fallbackFactory.create(ex), args);
                            return fallbackResult;
                        } catch (IllegalAccessException e) {
                            // shouldn't happen as method is public due to being an
                            // interface
                            throw new AssertionError(e);
                        } catch (InvocationTargetException e) {
                            throw new AssertionError(e.getCause());
                        }
                    } else {
                        // 增加判断返回类型，如果是IngotResponse，那么返回缺省降级结果
                        Class<?> returnType = method.getReturnType();
                        if (returnType == R.class) {
                            if (ex instanceof IngotFeignException) {
                                return ResponseWrapper.error(((IngotFeignException) ex).getCode(), ex.getLocalizedMessage());
                            }
                            return ResponseWrapper.error(BaseStatusCode.REQUEST_FALLBACK.getCode(), ex.getLocalizedMessage());
                        } else {
                            // throw exception if fallbackFactory is null
                            throw ex;
                        }
                    }
                } finally {
                    if (entry != null) {
                        entry.exit(1, args);
                    }
                    ContextUtil.exit();
                }
            }
        } else {
            // other target type using default strategy
            result = methodHandler.invoke(args);
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IngotSentinelInvocationHandler) {
            IngotSentinelInvocationHandler other = (IngotSentinelInvocationHandler) obj;
            return target.equals(other.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return target.toString();
    }

    static Map<Method, Method> toFallbackMethod(Map<Method, MethodHandler> dispatch) {
        Map<Method, Method> result = new LinkedHashMap<>();
        for (Method method : dispatch.keySet()) {
            method.setAccessible(true);
            result.put(method, method);
        }
        return result;
    }
}
