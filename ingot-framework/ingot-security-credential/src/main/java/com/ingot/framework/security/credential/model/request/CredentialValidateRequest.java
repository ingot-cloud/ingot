package com.ingot.framework.security.credential.model.request;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.credential.model.CredentialScene;
import lombok.Builder;
import lombok.Data;

/**
 * 凭证校验请求
 * <p>统一的凭证校验入口，使用方只需提供用户信息即可</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
@Data
@Builder
public class CredentialValidateRequest {

    /**
     * 校验场景（必填）
     */
    private CredentialScene scene;

    /**
     * 密码（必填）
     */
    private String password;

    /**
     * 用户ID（登录和修改密码场景必填）
     */
    private Long userId;

    /**
     * 用户名（可选，用于强度校验）
     */
    private String username;

    /**
     * 手机号（可选，用于强度校验）
     */
    private String phone;

    /**
     * 邮箱（可选，用于强度校验）
     */
    private String email;

    /**
     * 用户类型（可选）
     */
    private UserTypeEnum userType;
}
