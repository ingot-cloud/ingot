package com.ingot.framework.core.model.transform;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : AdminUSerUserInfoDtoTransform.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/27.</p>
 * <p>Time         : 上午9:13.</p>
 */
@Component
public class CommonTypeTransform {

    public String longToString(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    public Long stringToLong(String value) {
        return StrUtil.isEmpty(value) ? null : Long.valueOf(value);
    }

    public String intToString(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    public Integer stringToInteger(String value) {
        return StrUtil.isEmpty(value) ? null : Integer.valueOf(value);
    }
}
