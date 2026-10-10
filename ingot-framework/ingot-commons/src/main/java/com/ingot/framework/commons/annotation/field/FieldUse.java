package com.ingot.framework.commons.annotation.field;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>区分字段输出、实际写入和查询条件绑定，不能用响应绑定推断写入能力。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum FieldUse {
    READ("READ"), WRITE("WRITE"), FILTER("FILTER");
    /** JSON 和数据库的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;
    private static final Map<String, FieldUse> BY_VALUE = EnumUtils.index(values(), FieldUse::getValue);
    /** 解析稳定用途，未知类型拒绝。 */
    @JsonCreator
    public static FieldUse getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
