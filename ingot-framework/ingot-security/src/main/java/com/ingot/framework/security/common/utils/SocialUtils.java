package com.ingot.framework.security.common.utils;

import com.ingot.framework.common.constants.GlobalConstants;

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
     * @param socialType 社交类型 {@link com.ingot.framework.core.model.enums.SocialTypeEnum}
     * @param openId     社交登录唯一ID
     * @return 唯一码
     */
    public static String uniqueCode(String socialType, String openId) {
        return socialType.concat(GlobalConstants.AT).concat(openId);
    }

    /**
     * 提取唯一码
     *
     * @param uniqueCode 唯一码
     * @return 提取唯一码数组，第0个为socialType，第一个为openId
     */
    public static String[] extract(String uniqueCode) {
        return uniqueCode.split(GlobalConstants.AT);
    }
}
