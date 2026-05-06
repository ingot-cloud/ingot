package com.ingot.framework.dict.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;

/**
 * 字典统一访问入口。
 * <p>
 * 业务代码注入此接口即可使用字典能力，由 {@code DictClientAutoConfiguration} 在不同
 * 服务上下文中自动选择本地实现（PMS）或 RPC 实现（其它微服务）。
 * </p>
 *
 * @author jy
 * @since 2026/4/25
 */
public interface DictService {

    /**
     * 按字典编码查询启用项（按平台默认作用域）。
     *
     * @param dictCode 字典编码
     * @return 字典项列表（按 sort 升序）
     */
    default List<DictItem> items(String dictCode) {
        return items(dictCode, DictQuery.platform());
    }

    /**
     * 按字典编码查询启用项。
     *
     * @param dictCode 字典编码
     * @param query    作用域条件
     * @return 字典项列表（按 sort 升序）
     */
    List<DictItem> items(String dictCode, DictQuery query);

    /**
     * 批量按字典编码查询字典项，避免高频字典页面多次 RPC。
     *
     * @param dictCodes 字典编码集合
     * @param query     作用域条件
     * @return 以 dictCode 为 key、字典项列表为 value 的 Map
     */
    Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query);

    /**
     * 根据字典编码与值获取展示文本。
     *
     * @param dictCode 字典编码
     * @param value    字典项值
     * @return 展示文本（不存在时返回原始 value）
     */
    default String label(String dictCode, String value) {
        return label(dictCode, value, DictQuery.platform());
    }

    /**
     * 根据字典编码与值获取展示文本。
     */
    default String label(String dictCode, String value, DictQuery query) {
        if (value == null) {
            return null;
        }
        return items(dictCode, query).stream()
                .filter(item -> value.equals(item.getValue()))
                .findFirst()
                .map(DictItem::getLabel)
                .orElse(value);
    }

    /**
     * 字典项 value -> label 映射，便于前端表格回填。
     */
    default Map<String, String> labelMap(String dictCode, DictQuery query) {
        return items(dictCode, query).stream()
                .filter(item -> item.getValue() != null)
                .collect(Collectors.toMap(DictItem::getValue,
                        item -> item.getLabel() == null ? item.getValue() : item.getLabel(),
                        (a, b) -> a));
    }

    /**
     * 校验字典项值是否在指定字典中。
     */
    default boolean exists(String dictCode, String value, DictQuery query) {
        if (value == null) {
            return false;
        }
        return items(dictCode, query).stream()
                .anyMatch(item -> value.equals(item.getValue()));
    }

    /**
     * 根据 value 取出字典项详情。
     */
    default Optional<DictItem> findItem(String dictCode, String value, DictQuery query) {
        if (value == null) {
            return Optional.empty();
        }
        return items(dictCode, query).stream()
                .filter(item -> value.equals(item.getValue()))
                .findFirst();
    }

    /**
     * 强制刷新指定字典编码的本地缓存。
     */
    default void evict(String dictCode) {
        // default no-op
    }

    /**
     * 强制刷新所有字典缓存。
     */
    default void evictAll() {
        // default no-op
    }

    /**
     * 工具方法：按字典项任意字段提取 Map，例如：
     * <pre>{@code groupBy(items, DictItem::getValue)}</pre>
     */
    static <K> Map<K, DictItem> indexBy(List<DictItem> items, Function<DictItem, K> keyMapper) {
        return items.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a));
    }
}
