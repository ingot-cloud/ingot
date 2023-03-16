package com.ingot.framework.security.common.utils;

import com.ingot.framework.core.constants.GlobalConstants;
import com.ingot.framework.core.model.enums.SocialTypeEnums;

/**
 * <p>Description  : SocialUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/3.</p>
 * <p>Time         : 7:51 下午.</p>
 */
public final class SocialUtils {

    /**
     * 生成唯一码
     *
     * @param socialType 社交类型 {@link SocialTypeEnums}
     * @param code       社交登录code
     * @return 唯一码
     */
    public static String uniqueCode(String socialType, String code) {
        return socialType.concat(GlobalConstants.AT).concat(code);
    }

    /**
     * 提取唯一码
     *
     * @param uniqueCode 唯一码
     * @return 提取唯一码数组，第0个为socialType，第一个为code
     */
    public static String[] extract(String uniqueCode) {
        return uniqueCode.split(GlobalConstants.AT);
    }
}
