package com.ingot.framework.core.annotation;

import com.ingot.framework.core.config.MessageConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * <p>Description  : ImportMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/23.</p>
 * <p>Time         : 6:49 下午.</p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(MessageConfig.class)
public @interface ImportMessageSource {
}
