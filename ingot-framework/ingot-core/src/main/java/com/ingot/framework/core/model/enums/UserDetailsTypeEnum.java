package com.ingot.framework.core.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * <p>Description  : UserDetailsTypeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:01 下午.</p>
 */
public enum UserDetailsTypeEnum {
    PASSWORD("password", "密码登录"),
    SOCIAL("social", "社交登录");

    @Getter
    private final String value;
    @Getter
    private final String desc;

    UserDetailsTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static String getDesc(String value){
        UserDetailsTypeEnum en = getEnum(value);
        return en != null ? en.desc : null;
    }

    public static UserDetailsTypeEnum getEnum(String value){
        if (StrUtil.isEmpty(value)){
            return null;
        }
        UserDetailsTypeEnum[] arr = UserDetailsTypeEnum.values();
        for (UserDetailsTypeEnum item: arr){
            if (StrUtil.equals(item.value, value)){
                return item;
            }
        }

        return null;
    }
}
