package com.ingot.framework.commons.model.support;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : 选项.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/20.</p>
 * <p>Time         : 3:28 PM.</p>
 */
@Data
public class Option<Value> implements Serializable {
    /**
     * 选项值
     */
    private Value value;
    /**
     * 选项名称
     */
    private String label;

    public static <T> Option<T> of(T value, String label) {
        Option<T> option = new Option<>();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }
}
