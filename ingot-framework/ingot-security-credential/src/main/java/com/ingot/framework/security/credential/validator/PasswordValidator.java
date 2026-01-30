package com.ingot.framework.security.credential.validator;

import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;

/**
 * 密码校验器接口
 *
 * @author jymot
 * @since 2026-01-24
 */
public interface PasswordValidator {

    /**
     * 校验密码
     *
     * @param context 校验上下文（包含场景、密码、用户信息等）
     * @return 校验结果
     */
    PasswordCheckResult validate(PolicyCheckContext context);
}
