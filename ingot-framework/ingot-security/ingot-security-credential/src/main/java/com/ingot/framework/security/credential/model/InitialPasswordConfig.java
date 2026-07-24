package com.ingot.framework.security.credential.model;

import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy;

/**
 * 生效的初始密码策略配置（不可变载体）。
 *
 * <p>由 {@code CredentialPolicyLoader} 依据当前模式（{@code remote} 优先 / {@code local} 兜底）解析后返回，
 * 供 {@code InitialPasswordService} 生成初始密码、判定有效期与首登强制改密，使初始密码与
 * strength / history / expiration 共享同一「安全中心优先、Nacos 兜底」的来源语义。</p>
 *
 * @param generation              初始密码生成方式
 * @param length                  随机密码长度
 * @param fixedPassword           统一默认密码（{@code generation=FIXED} 时生效）
 * @param validHours              初始密码有效小时数；{@code 0} 表示不限制
 * @param oneTime                 用后失效
 * @param forceChangeOnFirstLogin 首登是否强制修改密码
 * @author jy
 * @since 1.0.0
 */
public record InitialPasswordConfig(
        InitialPasswordPolicy.Generation generation,
        int length,
        String fixedPassword,
        int validHours,
        boolean oneTime,
        boolean forceChangeOnFirstLogin) {

    /**
     * 由本地属性构建。
     */
    public static InitialPasswordConfig from(InitialPasswordPolicy policy) {
        return new InitialPasswordConfig(
                policy.getGeneration(),
                policy.getLength(),
                policy.getFixedPassword(),
                policy.getValidHours(),
                policy.isOneTime(),
                policy.isForceChangeOnFirstLogin());
    }

    /**
     * 缺省配置（安全中心未下发且无本地覆盖时使用）。
     */
    public static InitialPasswordConfig defaults() {
        return from(new InitialPasswordPolicy());
    }
}
