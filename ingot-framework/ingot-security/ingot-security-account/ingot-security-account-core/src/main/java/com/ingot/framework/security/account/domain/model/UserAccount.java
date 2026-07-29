package com.ingot.framework.security.account.domain.model;

import java.time.LocalDateTime;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 用户账号领域模型
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@Builder
public class UserAccount {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户类型
     */
    private UserTypeEnum userType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码哈希
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 是否必须修改密码
     */
    private Boolean mustChangePwd;

    /**
     * 密码最后修改时间
     */
    private LocalDateTime passwordChangedAt;

    /**
     * 是否启用（1-启用 0-禁用）
     */
    private Boolean enabled;

    /**
     * 是否锁定（1-锁定 0-正常，详情见 account_lock_state）
     */
    private Boolean locked;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 乐观锁版本号
     */
    private Long version;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 检查账号是否可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled) 
            && !Boolean.TRUE.equals(locked) 
            && deletedAt == null;
    }

    /**
     * 检查是否已删除
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 检查是否锁定
     */
    public boolean isLocked() {
        return Boolean.TRUE.equals(locked);
    }
}
