package com.ingot.framework.base.model.enums;

import lombok.Getter;

/**
 * <p>Description  : CommonStatus.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/19.</p>
 * <p>Time         : 5:16 下午.</p>
 */
public enum CommonStatusEnum {

    ENABLE("0", "正常可用"),
    LOCK("9", "锁定");

    @Getter
    private final String value;
    @Getter
    private final String desc;

    CommonStatusEnum(String value, String desc){
        this.value = value;
        this.desc = desc;
    }
}
