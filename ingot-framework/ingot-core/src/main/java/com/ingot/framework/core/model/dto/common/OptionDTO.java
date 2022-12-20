package com.ingot.framework.core.model.dto.common;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : OptionDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/20.</p>
 * <p>Time         : 3:28 PM.</p>
 */
@Data
public class OptionDTO<Value> implements Serializable {
    /**
     * 选项值
     */
    private Value value;
    /**
     * 选项名称
     */
    private String label;
}
