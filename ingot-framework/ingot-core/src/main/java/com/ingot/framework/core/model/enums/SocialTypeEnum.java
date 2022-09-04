package com.ingot.framework.core.model.enums;

import cn.hutool.core.util.StrUtil;
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
    private final String desc;

    public static String getDesc(String value) {
        SocialTypeEnum en = getEnum(value);
        return en != null ? en.desc : null;
    }

    public static SocialTypeEnum getEnum(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        SocialTypeEnum[] arr = SocialTypeEnum.values();
        for (SocialTypeEnum item : arr) {
            if (StrUtil.equals(item.value, value)) {
                return item;
            }
        }

        return null;
    }
}
