package com.ingot.framework.security.core.userdetails;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>登录失败的自定义纯文本文案，每次解析从当前环境新绑定，未配置时使用安全消息资源。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = LoginFailureMessageProperties.PREFIX)
public class LoginFailureMessageProperties {
    /** 独立安全文案文件中的登录提示配置前缀。 */
    public static final String PREFIX = "ingot.security.login.messages";

    /** 初始／重置密码超期提示；缺省或空白时使用内置默认，不解析模板。 */
    private String initialPasswordExpired;

    /** 普通密码过期且已无宽限登录机会的提示；缺省或空白时使用内置默认。 */
    private String passwordExpired;

    /** 未知原因的凭证过期提示；缺省或空白时使用内置默认。 */
    private String credentialsExpired;
}
