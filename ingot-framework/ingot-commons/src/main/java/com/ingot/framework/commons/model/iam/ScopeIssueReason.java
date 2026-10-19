package com.ingot.framework.commons.model.iam;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>定义当前授权待补范围原因。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ScopeIssueReason {
    MISSING_BINDING("MISSING_BINDING"),
    INVALID_BINDING("INVALID_BINDING"),
    MISSING_RESOURCE_CEILING("MISSING_RESOURCE_CEILING");
    /** JSON及持久化的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;
    private static final Map<String, ScopeIssueReason> BY_VALUE = EnumUtils.index(values(), ScopeIssueReason::getValue);
    /** 解析已声明字面量，未知值明确拒绝。 */
    @JsonCreator
    public static ScopeIssueReason getEnum(String value) { return EnumUtils.require(BY_VALUE, value); }
}
