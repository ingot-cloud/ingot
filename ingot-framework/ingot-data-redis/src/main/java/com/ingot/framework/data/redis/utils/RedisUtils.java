package com.ingot.framework.data.redis.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * <p>Description  : RedisUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/2.</p>
 * <p>Time         : 09:23.</p>
 */
public class RedisUtils {

    /**
     * 获取缓存名称
     *
     * @param name 缓存名称
     * @return 返回结构化的名称，如果不忽略租户则在name前面追加租户
     */
    public static String getCacheName(String name) {
        if (StrUtil.startWith(name, CacheConstants.IGNORE_TENANT_PREFIX)) {
            return name;
        }

        return TenantContextHolder.get() + StrUtil.COLON + name;
    }

    /**
     * 删除key
     *
     * @param redisTemplate {@link RedisTemplate}
     * @param patterns      pattern
     */
    public static void deleteKeys(RedisTemplate<String, Object> redisTemplate, List<String> patterns) {
        if (CollUtil.isEmpty(patterns)) {
            return;
        }

        List<String> deleteKeys = patterns.stream()
                .flatMap(pattern ->
                        CollUtil.emptyIfNull(
                                redisTemplate.keys(RedisUtils.getCacheName(pattern))).stream())
                .toList();

        redisTemplate.delete(deleteKeys);
    }
}
