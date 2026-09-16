package com.ingot.cloud.iam.identity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>映射新环境冷启动配置，只控制受控平台身份的建立，不承载目录种子。</p>
 *
 * <p>应用、资源、操作、菜单、治理角色与默认策略由 {@code databases/iam/006_bootstrap.sql}
 * 写入；本配置默认关闭，仅在运维显式开启时建立首个平台账号与成员。初始口令由凭证框架的
 * 初始密码策略生成，不接受配置注入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = IamBootstrapProperties.PREFIX)
public class IamBootstrapProperties {
    /**
     * 配置前缀，同时供 {@code @ConditionalOnProperty} 使用。
     */
    public static final String PREFIX = "ingot.iam.bootstrap";
    /**
     * 开关属性名，注解需要编译期常量。
     */
    public static final String ENABLED = "enabled";

    /**
     * 是否执行冷启动。默认 false，只有新环境显式开启才建立平台身份。
     */
    private boolean enabled = false;
    /**
     * 首个平台账号登录名。默认 {@code platform}。
     */
    private String username = "platform";
    /**
     * 首个平台成员展示名。默认「平台治理」。
     */
    private String displayName = "平台治理";
    /**
     * 首个平台账号手机号，可空。
     */
    private String phone;
    /**
     * 首个平台账号邮箱，可空。
     */
    private String email;
}
