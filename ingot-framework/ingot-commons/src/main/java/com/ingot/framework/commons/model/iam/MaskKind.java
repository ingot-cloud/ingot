package com.ingot.framework.commons.model.iam;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>声明预设和参数化文本脱敏类型，不执行脚本或正则表达式。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MaskKind {
    PHONE("PHONE"), EMAIL("EMAIL"), ALL("ALL"), KEEP_EDGES("KEEP_EDGES"), RANGE("RANGE");

    /** JSON 和数据库的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;
    private static final Map<String, MaskKind> BY_VALUE = EnumUtils.index(values(), MaskKind::getValue);

    /** 按稳定字面量解析，未知类型拒绝。 */
    @JsonCreator
    public static MaskKind getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
