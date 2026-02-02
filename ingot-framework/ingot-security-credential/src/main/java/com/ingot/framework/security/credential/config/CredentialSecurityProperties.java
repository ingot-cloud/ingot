package com.ingot.framework.security.credential.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 凭证安全配置属性
 *
 * @author jymot
 * @since 2026-01-21
 */
@Data
@ConfigurationProperties(prefix = "ingot.credential")
public class CredentialSecurityProperties {

    /**
     * 策略配置
     */
    private PolicyConfig policy = new PolicyConfig();

    @Data
    public static class PolicyConfig {
        /**
         * 策略模式，local, remote
         */
        private String mode = "local";

        /**
         * 密码强度策略
         */
        private StrengthPolicy strength = new StrengthPolicy();

        /**
         * 密码过期策略
         */
        private ExpirationPolicy expiration = new ExpirationPolicy();

        /**
         * 密码历史策略
         */
        private HistoryPolicy history = new HistoryPolicy();
    }

    @Data
    public static class ExpirationPolicy {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 最大有效天数
         */
        private int maxDays = 90;

        /**
         * 提前警告天数
         */
        private int warningDaysBefore = 7;

        /**
         * 宽限登录次数
         */
        private int graceLoginCount = 3;

        /**
         * 管理员重置后是否强制修改
         */
        private boolean forceChangeAfterReset = true;
    }

    @Data
    public static class HistoryPolicy {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 检查最近N次密码
         */
        private int checkCount = 5;
    }

    @Data
    public static class StrengthPolicy {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 最小长度
         */
        private int minLength = 8;

        /**
         * 最大长度
         */
        private int maxLength = 32;

        /**
         * 是否要求大写字母
         */
        private boolean requireUppercase = true;

        /**
         * 是否要求小写字母
         */
        private boolean requireLowercase = true;

        /**
         * 是否要求数字
         */
        private boolean requireDigit = true;

        /**
         * 是否要求特殊字符
         */
        private boolean requireSpecialChar = false;

        /**
         * 特殊字符集合
         */
        private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        /**
         * 禁止的模式
         */
        private List<String> forbiddenPatterns = new ArrayList<>();

        /**
         * 禁止包含用户属性
         */
        private boolean forbidUserAttributes = true;
    }
}
