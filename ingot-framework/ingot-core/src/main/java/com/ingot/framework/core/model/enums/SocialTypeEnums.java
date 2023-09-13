package com.ingot.framework.core.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : SocialTypeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:20 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum SocialTypeEnums {
    ADMIN_SMS("admin_sms", "短信登录"),
    ADMIN_WECHAT("admin_wechat", "微信登录"),
    ADMIN_MINI_PROGRAM("admin_miniprogram", "微信小程序"),

    APP_SMS("app_sms", "短信登录"),
    APP_WECHAT("app_wechat", "微信登录"),
    APP_MINI_PROGRAM("app_miniprogram", "微信小程序");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, SocialTypeEnums> valueMap = new HashMap<>();

    static {
        for (SocialTypeEnums item : SocialTypeEnums.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static SocialTypeEnums get(String value) {
        return valueMap.get(value);
    }

    public static String getText(String value) {
        SocialTypeEnums en = get(value);
        return en != null ? en.text : null;
    }
}
