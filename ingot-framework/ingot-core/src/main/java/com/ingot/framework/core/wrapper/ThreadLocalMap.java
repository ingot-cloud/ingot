package com.ingot.framework.core.wrapper;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.UtilityClass;

/**
 * <p>Description  : ThreadLocalMap.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/21.</p>
 * <p>Time         : 5:52 下午.</p>
 */
@UtilityClass
public class ThreadLocalMap {
    /**
     * The constant threadContext.
     */
    private final static ThreadLocal<Map<String, Object>> THREAD_CONTEXT = new MapThreadLocal();

    /**
     * Put.
     *
     * @param key   the key
     * @param value the value
     */
    public void put(String key, Object value) {
        getContextMap().put(key, value);
    }

    /**
     * Remove object.
     *
     * @param key the key
     * @return the object
     */
    public Object remove(String key) {
        return getContextMap().remove(key);
    }

    /**
     * Get object.
     *
     * @param key the key
     * @return the object
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) getContextMap().get(key);
    }

    /**
     * 清理线程所有被hold住的对象。以便重用！
     */
    public void clear() {
        getContextMap().clear();
    }

    /**
     * 取得thread context Map的实例。
     *
     * @return thread context Map的实例
     */
    private Map<String, Object> getContextMap() {
        return THREAD_CONTEXT.get();
    }

    private static class MapThreadLocal extends TransmittableThreadLocal<Map<String, Object>> {
        /**
         * Initial value map.
         *
         * @return the map
         */
        @Override protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>(8) {

                private static final long serialVersionUID = 3637958959138295593L;

                @Override
                public Object put(String key, Object value) {
                    return super.put(key, value);
                }
            };
        }
    }
}

