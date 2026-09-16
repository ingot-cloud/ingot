package com.ingot.framework.commons.model.security;

import java.io.Serializable;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.Data;

/**
 * <p>Description  : UserDetailsRequest.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:47 下午.</p>
 */
@Data
public class UserDetailsRequest implements Serializable {
    /**
     * 身份类型
     */
    private UserIdentityTypeEnum type;
    /**
     * 唯一编码，根据类型判断，可以是用户名或手机号或社交openId等
     */
    private String username;
    /**
     * 授权类型
     */
    private String grantType;
    /**
     * 用户类型
     */
    private UserTypeEnum userType;
    /**
     * 登录的tenant，可以为空
     */
    private Long tenant;
    /**
     * 认证入口声明的管理域，可以为空。
     * <p>{@code PLATFORM} 为平台身份且 {@code tenant} 必须为空；{@code TENANT} 携带 {@code tenant}
     * 时选择该组织成员，不携带时为成员资格选择阶段。为空时按既有行为兼容处理，
     * 由身份提供方而非本 DTO 决定推断规则。</p>
     */
    private AuthorizationDomain domain;
    /**
     * 社交类型, {@code grantType} 为 com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType#SOCIAL 时，不为空
     */
    private SocialTypeEnum socialType;
    /**
     * 社交code, {@code grantType} 为 com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType#SOCIAL 时，不为空
     */
    private String socialCode;
}
