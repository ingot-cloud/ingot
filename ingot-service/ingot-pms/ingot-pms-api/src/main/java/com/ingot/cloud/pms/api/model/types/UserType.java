package com.ingot.cloud.pms.api.model.types;

import com.ingot.framework.commons.model.enums.UserStatusEnum;

/**
 * <p>Description  : UserType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/5/7.</p>
 * <p>Time         : 16:59.</p>
 */
public interface UserType {
    /**
     * ID
     */
    Long getId();

    /**
     * 用户名
     */
    String getUsername();

    /**
     * 昵称
     */
    String getNickname();

    /**
     * 手机号
     */
    String getPhone();

    /**
     * 邮件地址
     */
    String getEmail();

    /**
     * 头像
     */
    String getAvatar();

    /**
     * 状态, 0:正常，9:禁用
     */
    UserStatusEnum getStatus();

    /**
     * 设置状态
     *
     * @param status 状态
     */
    void setStatus(UserStatusEnum status);
}
