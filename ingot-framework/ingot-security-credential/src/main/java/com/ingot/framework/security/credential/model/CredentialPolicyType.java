package com.ingot.framework.security.credential.model;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : CredentialPolicy.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/26.</p>
 * <p>Time         : 10:04.</p>
 */
@Getter
@RequiredArgsConstructor
public enum CredentialPolicyType {
    STRENGTH("1", "密码强度"),
    HISTORY("2", "密码历史"),
    EXPIRATION("3", "密码过期");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, CredentialPolicyType> valueMap = new HashMap<>();
    private static final Map<String, CredentialPolicyType> textMap = new HashMap<>();

    static {
        for (CredentialPolicyType item : CredentialPolicyType.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static CredentialPolicyType getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static CredentialPolicyType getEnum(String value) {
        return valueMap.get(value);
    }
}
