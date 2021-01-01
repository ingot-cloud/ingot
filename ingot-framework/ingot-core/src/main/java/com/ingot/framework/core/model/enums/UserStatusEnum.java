package com.ingot.framework.core.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>Description  : UserStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/19.</p>
 * <p>Time         : 5:16 下午.</p>
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    ENABLE("0", "正常可用"),
    LOCK("9", "已锁定");

    /**
     * 状态
     */
    @JsonValue
    @EnumValue
    private final String value;

    /**
     * 描述
     */
    private final String desc;
}
