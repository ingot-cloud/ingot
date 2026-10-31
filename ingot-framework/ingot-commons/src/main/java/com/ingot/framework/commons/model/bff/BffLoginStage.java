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
 * 登录事务的服务端状态。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum BffLoginStage {

    LOGIN("LOGIN"), AUTHENTICATING("AUTHENTICATING"), SELECT_TENANT("SELECT_TENANT"), COMMITTING("COMMITTING"),
    COMPLETED("COMPLETED"), FAILED("FAILED"), EXPIRED("EXPIRED"), UNKNOWN("UNKNOWN");

    /** JSON 与存储使用的稳定协议字面量。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, BffLoginStage> BY_VALUE = EnumUtils.index(values(), BffLoginStage::getValue);

    /** 严格解析协议值，未知非空值拒绝。 */
    @JsonCreator
    public static BffLoginStage getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

}
