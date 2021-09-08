package com.ingot.framework.security.common.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * <p>Description  : ObjectUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/28.</p>
 * <p>Time         : 10:47 上午.</p>
 */
public final class ObjectUtils {

    /**
     * 将Object转为String
     * 如果target为空那么返回空字符串
     *
     * @param target 目标对象
     * @return String
     */
    public static String toString(Object target) {
        if (null == target) {
            return StrUtil.EMPTY;
        }
        if (target instanceof Map) {
            return target.toString();
        }

        return Convert.toStr(target);
    }
}
