package com.ingot.framework.security.credential.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 凭证校验场景
 *
 * @author jymot
 * @since 2026-01-24
 */
@Getter
@RequiredArgsConstructor
public enum CredentialScene {

    /**
     * 用户注册
     * <p>校验：密码强度</p>
     */
    REGISTER("注册", "REGISTER"),

    /**
     * 修改密码
     * <p>校验：密码强度、密码历史</p>
     */
    CHANGE_PASSWORD("修改密码", "CHANGE_PASSWORD"),

    /**
     * 登录验证
     * <p>校验：密码过期</p>
     */
    LOGIN("登录", "LOGIN"),

    /**
     * 重置密码
     * <p>预留该状态，目前重置密码为管理员操作，会自动生成随机密码然后用户登录强制修改，所以不用走密码策略</p>
     */
    RESET_PASSWORD("重置密码", "RESET_PASSWORD");

    /**
     * 场景名称
     */
    private final String name;

    /**
     * 场景代码
     */
    private final String code;

    @Override
    public String toString() {
        return this.code;
    }
}
