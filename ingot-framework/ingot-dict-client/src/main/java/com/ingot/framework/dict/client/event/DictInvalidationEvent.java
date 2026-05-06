package com.ingot.framework.dict.client.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingot.framework.eventbus.EventType;
import com.ingot.framework.eventbus.InvalidationEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 字典失效事件。dictCode 为 {@code null} 或 {@link #all} 为 {@code true} 时表示全量失效。
 *
 * @author jy
 * @since 2026/4/27
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EventType("dict.invalidate")
public class DictInvalidationEvent extends InvalidationEvent {

    /**
     * 字典编码；为空且 {@link #all} 为 {@code true} 时清空全部字典缓存。
     */
    private String dictCode;

    /**
     * 是否全量失效。
     */
    private boolean all;

    @JsonCreator
    public DictInvalidationEvent(@JsonProperty("dictCode") String dictCode,
                                 @JsonProperty("all") boolean all) {
        this.dictCode = dictCode;
        this.all = all;
    }

    public static DictInvalidationEvent of(String dictCode) {
        return new DictInvalidationEvent(dictCode, false);
    }

    public static DictInvalidationEvent all() {
        return new DictInvalidationEvent(null, true);
    }
}
