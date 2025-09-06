package com.ingot.framework.commons.model.transform;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : CommonTypeTransform.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/27.</p>
 * <p>Time         : 上午9:13.</p>
 */
public class CommonTypeTransform {

    public String longToString(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    public Long stringToLong(String value) {
        // 避免金额格式化，去掉逗号和句号
        return StrUtil.isEmpty(value) ? null : Long.valueOf(value
                .replaceAll(",", "")
                .replaceAll("\\.", ""));
    }

    public String intToString(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    public Integer stringToInteger(String value) {
        return StrUtil.isEmpty(value) ? null : Integer.valueOf(value);
    }
}
