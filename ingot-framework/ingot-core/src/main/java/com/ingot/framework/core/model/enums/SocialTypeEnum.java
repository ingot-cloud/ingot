package com.ingot.framework.core.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.ingot.framework.core.constants.SocialConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : SocialTypeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:20 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum SocialTypeEnum {
    SMS("sms", SocialConstants.BEAN_PHONE, "短信登录"),
    WECHAT("wechat", SocialConstants.BEAN_WECHAT, "微信登录");

    private final String value;
    private final String beanName;
    private final String text;

    private static final Map<String, SocialTypeEnum> valueMap = new HashMap<>();

    static {
        for (SocialTypeEnum item : SocialTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    public static SocialTypeEnum get(String value) {
        return valueMap.get(value);
    }

    public static String getText(String value) {
        SocialTypeEnum en = get(value);
        return en != null ? en.text : null;
    }
}
