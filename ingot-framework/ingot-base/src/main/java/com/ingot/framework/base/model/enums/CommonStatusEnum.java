package com.ingot.framework.base.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>Description  : CommonStatus.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/19.</p>
 * <p>Time         : 5:16 下午.</p>
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum {

    ENABLE("0", "正常可用"),
    LOCK("9", "锁定");

    /**
     * 状态
     */
    private final String value;

    /**
     * 描述
     */
    private final String desc;
}
