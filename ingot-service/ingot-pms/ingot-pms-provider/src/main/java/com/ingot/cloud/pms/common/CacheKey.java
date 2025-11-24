package com.ingot.cloud.pms.common;

/**
 * <p>Description  : CacheKey.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/27.</p>
 * <p>Time         : 11:47 AM.</p>
 */
public interface CacheKey {

    /**
     * 缓存默认过期时间,单位：秒, 默认缓存1周
     */
    String DefaultExpiredTimeSeconds = "604800";

    String ClientListKey = "'list'";

    /**
     * 公共缓存Key
     */
    String ListKey = "'list'";
    String ItemKey = "'item-' + #id";
    String CodeKey = "'code-' + #code";
}
