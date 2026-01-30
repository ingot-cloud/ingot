package com.ingot.framework.security.credential.model;

import java.time.LocalDateTime;
import java.util.List;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 策略校验上下文
 *
 * @author jymot
 * @since 2026-01-21
 */
@Data
@Builder
public class PolicyCheckContext {

    /**
     * 校验场景
     */
    @Builder.Default
    private CredentialScene scene = CredentialScene.GENERAL;

    /**
     * 待校验的密码（明文）
     */
    private String password;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户类型
     */
    private UserTypeEnum userType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 历史密码哈希列表（用于历史密码检查）
     * <p>场景：修改密码</p>
     */
    private List<String> oldPasswordHashes;

    /**
     * 最后修改密码时间（用于过期检查）
     * <p>场景：登录</p>
     */
    private LocalDateTime lastPasswordChangedAt;

    /**
     * 是否强制修改密码
     * <p>场景：登录</p>
     */
    private Boolean forcePasswordChange;

    /**
     * 剩余宽限登录次数
     * <p>场景：登录</p>
     */
    private Integer graceLoginRemaining;
}
