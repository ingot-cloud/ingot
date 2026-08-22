package com.ingot.cloud.security.api.model.enums;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ingot.framework.security.event.codes.SecurityEventCategoryCodes;
import com.ingot.framework.security.event.codes.SecurityEventCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link SecurityEventType} / {@link SecurityEventCategory} 与 code 常量往返单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class SecurityEventTypeTest {

    @Test
    @DisplayName("全部类型 code 与 SecurityEventCodes 同名常量逐字一致，且 fromCode 可往返")
    void typeCodesRoundTripAllConstants() throws Exception {
        Set<String> constantNames = stringConstantNames(SecurityEventCodes.class);
        assertThat(constantNames).hasSize(SecurityEventType.values().length);

        for (SecurityEventType type : SecurityEventType.values()) {
            Field field = SecurityEventCodes.class.getField(type.name());
            String constant = (String) field.get(null);
            assertThat(type.getCode()).isEqualTo(constant);
            assertThat(SecurityEventType.fromCode(constant)).isEqualTo(type);
            assertThat(SecurityEventType.fromCode(" " + constant + " ")).isEqualTo(type);
        }
    }

    @Test
    @DisplayName("全部分类 code 与 SecurityEventCategoryCodes 同名常量逐字一致，且 fromCode 可往返")
    void categoryCodesRoundTripAllConstants() throws Exception {
        Set<String> constantNames = stringConstantNames(SecurityEventCategoryCodes.class);
        assertThat(constantNames).hasSize(SecurityEventCategory.values().length);

        for (SecurityEventCategory category : SecurityEventCategory.values()) {
            Field field = SecurityEventCategoryCodes.class.getField(category.name());
            String constant = (String) field.get(null);
            assertThat(category.getCode()).isEqualTo(constant);
            assertThat(SecurityEventCategory.fromCode(constant)).isEqualTo(category);
            assertThat(SecurityEventCategory.fromCode(constant.toLowerCase())).isEqualTo(category);
        }
    }

    @Test
    @DisplayName("fromCode 对空白与未知 code 返回 null")
    void fromCodeRejectsUnknown() {
        assertThat(SecurityEventType.fromCode(null)).isNull();
        assertThat(SecurityEventType.fromCode("  ")).isNull();
        assertThat(SecurityEventType.fromCode("UNKNOWN_TYPE")).isNull();
        assertThat(SecurityEventCategory.fromCode(null)).isNull();
        assertThat(SecurityEventCategory.fromCode("UNKNOWN")).isNull();
    }

    private static Set<String> stringConstantNames(Class<?> type) {
        Set<String> names = new LinkedHashSet<>();
        for (Field field : type.getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType() == String.class) {
                names.add(field.getName());
            }
        }
        return names;
    }
}
