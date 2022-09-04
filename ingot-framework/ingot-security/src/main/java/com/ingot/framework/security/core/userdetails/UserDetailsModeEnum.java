package com.ingot.framework.security.core.userdetails;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;
import lombok.Getter;

/**
 * <p>Description  : UserDetailsModeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:01 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserDetailsModeEnum {
    PASSWORD("password", "密码登录"),
    SOCIAL("social", "社交登录");

    @JsonValue
    private final String value;
    private final String desc;

    public static String getDesc(String value){
        UserDetailsModeEnum en = getEnum(value);
        return en != null ? en.desc : null;
    }

    public static UserDetailsModeEnum getEnum(String value){
        if (StrUtil.isEmpty(value)){
            return null;
        }
        UserDetailsModeEnum[] arr = UserDetailsModeEnum.values();
        for (UserDetailsModeEnum item: arr){
            if (StrUtil.equals(item.value, value)){
                return item;
            }
        }

        return null;
    }
}
