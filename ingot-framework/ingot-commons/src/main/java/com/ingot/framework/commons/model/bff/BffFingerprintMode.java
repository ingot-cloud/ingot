package com.ingot.framework.commons.model.bff;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * BFF 会话的设备指纹来源。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum BffFingerprintMode {

    DEVICE(BffFingerprintMode.VALUE_DEVICE), IPUA(BffFingerprintMode.VALUE_IPUA);

    /** Nacos设备模式的稳定字面量。 */
    public static final String VALUE_DEVICE = "device";

    /** Nacos可信IP与UA模式的稳定字面量。 */
    public static final String VALUE_IPUA = "ip_ua";

    /** JSON 与存储使用的稳定协议字面量。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, BffFingerprintMode> BY_VALUE = EnumUtils.index(values(),
            BffFingerprintMode::getValue);

    /** 严格解析协议值，未知非空值拒绝。 */
    @JsonCreator
    public static BffFingerprintMode getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

}
