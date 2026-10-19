package com.ingot.framework.commons.model.iam;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>定义组织受限候选的封闭种类。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum TenantAssignmentOptionKind {
    ROLE("ROLE"),
    MEMBER("MEMBER"),
    GROUP("GROUP"),
    RESOURCE("RESOURCE"),
    APPLICATION("APPLICATION"),
    DEPARTMENT("DEPARTMENT");
    /** JSON及持久化的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;
    private static final Map<String, TenantAssignmentOptionKind> BY_VALUE = EnumUtils.index(values(), TenantAssignmentOptionKind::getValue);
    /** 解析已声明字面量，未知值明确拒绝。 */
    @JsonCreator
    public static TenantAssignmentOptionKind getEnum(String value) { return EnumUtils.require(BY_VALUE, value); }
}
