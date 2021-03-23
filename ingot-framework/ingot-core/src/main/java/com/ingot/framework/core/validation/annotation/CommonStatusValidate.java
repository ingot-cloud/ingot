package com.ingot.framework.core.validation.annotation;

import com.ingot.framework.core.validation.constraintvalidators.CommonStatusValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Description  : CommonStatusValidate.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 5:25 下午.</p>
 */
@Documented
@Constraint(validatedBy = {CommonStatusValidator.class})
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface CommonStatusValidate {

    String message() default "Validate.CommonStatusValidate";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
