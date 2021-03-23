package com.ingot.framework.core.validation.constraintvalidators;

import cn.hutool.core.util.StrUtil;
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
public class CommonStatusValidator implements ConstraintValidator<CommonStatusValidate, String> {

    @Override
    public void initialize(CommonStatusValidate constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return false;
        }

        CommonStatusEnum[] values = CommonStatusEnum.values();
        for (CommonStatusEnum item: values) {
            if (StrUtil.equals(item.getValue(), value)) {
                return true;
            }
        }
        return false;
    }
}
