package com.ingot.framework.commons.model.iam.extension;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.*;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * ObjectQueryPurpose稳定业务取值。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ObjectQueryPurpose {

    ASSIGNMENT("ASSIGNMENT"), DELEGATION("DELEGATION"), DIAGNOSE("DIAGNOSE");

    /**
     * 稳定契约字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, ObjectQueryPurpose> INDEX = EnumUtils.index(values(),
            ObjectQueryPurpose::getValue);

    /**
     * 解析稳定字面量。
     * @param value 输入
     * @return 枚举
     */
    @JsonCreator
    public static ObjectQueryPurpose getEnum(String value) {
        return EnumUtils.require(INDEX, value);
    }

}
