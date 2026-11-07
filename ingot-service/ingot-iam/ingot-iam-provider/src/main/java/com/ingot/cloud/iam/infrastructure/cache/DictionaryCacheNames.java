package com.ingot.cloud.iam.infrastructure.cache;

import com.ingot.framework.commons.constants.CacheConstants;

/**
 * <p>声明字典实体缓存的版本和键，隔离旧Java实体的JDK序列化载荷。</p>
 * @author wangchao
 * @since 2022/12/27
 */
public final class DictionaryCacheNames {
    /** 新实体类型专用缓存，不读取旧PlatformDict序列化数据。 */
    public static final String ENTITY_CACHE = CacheConstants.PLATFORM_DICTS + ":entity-v2";
    /** 字典全量节点缓存键。 */
    public static final String LIST_KEY = "'list'";
    /** 字典单节点缓存键，id来自受控方法参数。 */
    public static final String NODE_KEY = "'item-' + #id";

    private DictionaryCacheNames() { }
}
