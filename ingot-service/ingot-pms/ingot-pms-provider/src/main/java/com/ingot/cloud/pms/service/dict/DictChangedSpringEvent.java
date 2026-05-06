package com.ingot.cloud.pms.service.dict;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 字典变更的本地 Spring 事件，由 PMS 写操作发布；事务提交后由
 * {@link DictInvalidationPublisher} 转发为跨节点失效广播。
 *
 * @author jy
 * @since 2026/4/27
 */
@Getter
public class DictChangedSpringEvent extends ApplicationEvent {

    /**
     * 字典编码。{@code null} 或 {@link #all} 为 {@code true} 时表示全量失效。
     */
    private final String dictCode;

    /**
     * 是否全量失效（对所有字典编码生效）。
     */
    private final boolean all;

    public DictChangedSpringEvent(Object source, String dictCode, boolean all) {
        super(source);
        this.dictCode = dictCode;
        this.all = all;
    }

    public static DictChangedSpringEvent of(Object source, String dictCode) {
        return new DictChangedSpringEvent(source, dictCode, false);
    }

    public static DictChangedSpringEvent all(Object source) {
        return new DictChangedSpringEvent(source, null, true);
    }
}
