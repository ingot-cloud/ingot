package com.ingot.cloud.iam.support;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.ObjectCapability;
import com.ingot.framework.commons.model.iam.ResourceDetail;

/**
 * <p>构造字段策略接入前的资源详情，能力映射仅用于展示。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamDetails {
    private IamDetails() {
    }

    /**
     * 返回不含字段访问说明的详情。
     *
     * @param record 已投影记录
     * @param version 乐观锁版本
     * @param <T> 记录类型
     * @return 详情信封
     */
    public static <T> ResourceDetail<T> of(T record, String version) {
        return of(record, Map.of(), Map.of(), version);
    }

    /**
     * 返回带展示能力的详情。
     *
     * @param record 已投影记录
     * @param capabilities 按操作码索引的展示能力
     * @param version 乐观锁版本
     * @param <T> 记录类型
     * @return 详情信封
     */
    public static <T> ResourceDetail<T> of(T record, Map<String, ObjectCapability> capabilities, String version) {
        return of(record, Map.of(), capabilities, version);
    }

    /**
     * 返回完整详情快照。
     *
     * @param record 已投影记录
     * @param fieldAccess 字段访问
     * @param capabilities 展示能力
     * @param version 乐观锁版本
     * @param <T> 记录类型
     * @return 详情信封
     */
    public static <T> ResourceDetail<T> of(T record, Map<String, FieldAccess> fieldAccess,
                                          Map<String, ObjectCapability> capabilities, String version) {
        return new ResourceDetail<>(record, new LinkedHashMap<>(fieldAccess),
                new LinkedHashMap<>(capabilities), version);
    }
}
