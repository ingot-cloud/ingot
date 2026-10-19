package com.ingot.framework.commons.model.iam;

import java.util.Map;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>组织成员选择的候选资格来源；界面统一不改变各场景的授权边界。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum OrganizationMemberPickerScene {
    MEMBER("MEMBER"),
    DEPARTMENT_LEADER("DEPARTMENT_LEADER"),
    ASSIGNMENT("ASSIGNMENT");
    /** JSON及持久化的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;
    private static final Map<String, OrganizationMemberPickerScene> BY_VALUE = EnumUtils.index(values(), OrganizationMemberPickerScene::getValue);
    /** 拒绝未声明的候选场景。 */
    @JsonCreator
    public static OrganizationMemberPickerScene getEnum(String value) { return EnumUtils.require(BY_VALUE, value); }
}
