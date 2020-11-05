package com.ingot.framework.core.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * <p>Description  : SocialTypeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:20 下午.</p>
 */
public enum SocialTypeEnum {
    SMS("sms", "短信登录"),
    WECHAT("wechat", "微信登录");

    @Getter
    private final String value;
    @Getter
    private final String desc;

    SocialTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static String getDesc(String value){
        SocialTypeEnum en = getEnum(value);
        return en != null ? en.desc : null;
    }

    public static SocialTypeEnum getEnum(String value){
        if (StrUtil.isEmpty(value)){
            return null;
        }
        SocialTypeEnum[] arr = SocialTypeEnum.values();
        for (SocialTypeEnum item: arr){
            if (StrUtil.equals(item.value, value)){
                return item;
            }
        }

        return null;
    }
}
