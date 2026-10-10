package com.ingot.framework.authorization.field;

import com.ingot.framework.commons.model.iam.MaskSpec;

/**
 * <p>无数据库和网络访问的脱敏执行扩展点，不保留业务原值。</p>
 * @author jy
 * @since 1.0.0
 */
@FunctionalInterface
public interface MaskStrategy {
    /** 按已经校验的规则返回脱敏文本；null 保留空值语义。 */
    String mask(String value, MaskSpec spec);
}
