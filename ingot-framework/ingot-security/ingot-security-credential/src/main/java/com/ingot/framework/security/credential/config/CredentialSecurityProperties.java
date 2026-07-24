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
@ConfigurationProperties(prefix = "ingot.security.credential")
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
         * 降级兜底配置（{@code remote} 模式生效）
         */
        private Fallback fallback = new Fallback();

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

        /**
         * 初始密码策略
         */
        private InitialPasswordPolicy initialPassword = new InitialPasswordPolicy();
    }

    @Data
    public static class Fallback {
        /**
         * 远程不可用且无 LKG 时，是否落 Nacos 本地地板（可用性优先，默认 true）。
         * <p>置为 {@code false} 时无 LKG 场景将向上抛出远程不可用异常（极端严格场景）。</p>
         */
        private boolean localFloorEnabled = true;
    }

    @Data
    public static class InitialPasswordPolicy {
        /**
         * 初始密码生成方式：RANDOM-随机生成；FIXED-统一默认密码
         */
        private Generation generation = Generation.RANDOM;

        /**
         * 随机初始密码长度（generation=RANDOM 时生效）
         */
        private int length = 10;

        /**
         * 统一默认密码（generation=FIXED 时生效）
         */
        private String fixedPassword = "In@123456";

        /**
         * 初始密码有效小时数；0 表示不限制。超过后视为初始密码失效，需重新重置。
         */
        private int validHours = 72;

        /**
         * 用后失效：初始密码修改成功后即失效（依赖 mustChangePwd/force_change 清除，默认 true）
         */
        private boolean oneTime = true;

        /**
         * 首次登录是否强制修改密码
         */
        private boolean forceChangeOnFirstLogin = true;

        /**
         * 初始密码生成方式枚举
         */
        public enum Generation {
            /**
             * 随机生成
             */
            RANDOM,
            /**
             * 统一默认密码
             */
            FIXED
        }
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
