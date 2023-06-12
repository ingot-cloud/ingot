package com.ingot.framework.vc.common;

import java.lang.annotation.*;

/**
 * <p>Description  : VCVerify.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/12.</p>
 * <p>Time         : 3:00 PM.</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VCVerify {

    /**
     * 验证码类型
     *
     * @return {@link VCType}
     */
    VCType type();
}
