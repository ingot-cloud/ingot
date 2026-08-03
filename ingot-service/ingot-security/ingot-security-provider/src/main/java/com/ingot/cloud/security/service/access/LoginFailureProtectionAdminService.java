package com.ingot.cloud.security.service.access;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.cloud.security.model.domain.LoginFailureProtectionPolicy;

import java.util.List;

/**
 * 登录失败保护策略管理面 Service。
 *
 * @author jy
 * @since 1.0.0
 */
public interface LoginFailureProtectionAdminService {

    List<LoginFailureProtectionPolicy> list();

    LoginFailureProtectionPolicy getByDimension(LoginFailureDimension dimension);

    LoginFailureProtectionPolicy upsert(LoginFailureProtectionPolicy policy);
}
