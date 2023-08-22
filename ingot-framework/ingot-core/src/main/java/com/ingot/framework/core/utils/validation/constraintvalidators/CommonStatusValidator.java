package com.ingot.framework.core.utils.validation.constraintvalidators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.annotation.CommonStatusValidate;

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
