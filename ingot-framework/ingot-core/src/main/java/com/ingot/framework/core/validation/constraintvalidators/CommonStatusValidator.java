package com.ingot.framework.core.validation.constraintvalidators;

import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.validation.annotation.CommonStatusValidate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * <p>Description  : CommonStatusValidator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 5:27 下午.</p>
 */
public class CommonStatusValidator implements ConstraintValidator<CommonStatusValidate, CommonStatusEnum> {

    @Override
    public boolean isValid(CommonStatusEnum value, ConstraintValidatorContext context) {
        return value != null;
    }
}
