package com.ingot.framework.security.access.config;

import com.ingot.framework.commons.model.security.PolicySourceMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>访问防护配置属性，绑定 {@code ingot.security.access}，控制登录失败保护的策略来源与 Nacos 地板。</p>
 *
 * <p>{@code mode=local} 时直接使用 {@link #loginFailure}；{@code mode=remote} 时从安全中心拉取四维策略，
 * 远端不可用则按 {@code remote → LKG → 地板} 降级，地板内容同样来自 {@link #loginFailure}。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     access:
 *       mode: remote
 *       policy:
 *         fallback:
 *           local-floor-enabled: true
 *       login-failure:
 *         ip:
 *           enabled: true
 *           max-attempts: 50
 *           window-minutes: 1
 *           block-ttl-sec: 3600
 *           block-key-type: IP
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 *
 * @see com.ingot.framework.security.access.model.LoginFailurePolicy
 *
 * @apiNote 本前缀配在 Auth 服务 {@code in-service-auth.yml}，不配在 Gateway。
 *          未部署安全中心时保持 {@code local}；生产部署后切 {@code remote}。
 */
@Data
@ConfigurationProperties(prefix = "ingot.security.access")
public class AccessProtectionProperties {

    /**
     * 策略来源：{@link PolicySourceMode#LOCAL} 读本类 {@link #loginFailure}，
     * {@link PolicySourceMode#REMOTE} 走安全中心。
     * <p>代码缺省为 {@link PolicySourceMode#LOCAL}，便于未部署安全中心的环境启动；
     * 生产建议切 {@link PolicySourceMode#REMOTE}。</p>
     */
    private PolicySourceMode mode = PolicySourceMode.LOCAL;

    /**
     * remote 模式的降级参数；当前仅包含是否允许回落 Nacos 地板。
     */
    private PolicyConfig policy = new PolicyConfig();

    /**
     * 登录失败四维策略。
     * <p>{@code mode=local} 时直接生效；{@code mode=remote} 时仅作为远端与 LKG 均不可用时的 Nacos 地板。</p>
     */
    private LoginFailureConfig loginFailure = new LoginFailureConfig();

    /**
     * <p>remote 模式的降级参数。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class PolicyConfig {
        /**
         * 远端不可用时的末级回落。
         */
        private Fallback fallback = new Fallback();
    }

    /**
     * <p>远端不可用且无 LKG 时的末级回落开关。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class Fallback {
        /**
         * 远端不可用且无 LKG 时，是否回落 {@link LoginFailureConfig} 地板。
         * <p>关闭后该场景向上抛出远程不可用异常，而不是静默使用本地地板。</p>
         */
        private boolean localFloorEnabled = true;
    }

    /**
     * <p>登录失败保护的四维策略：IP、设备、OAuth2 Client、账号+IP。</p>
     *
     * <p>各维度独立计数与封禁；达阈值后仅写网关共用临时封禁，不触发账号 lockout。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class LoginFailureConfig {
        /**
         * 同一 IP 的失败保护；默认 1 分钟内 50 次失败后封禁 3600 秒，封禁键类型 {@code IP}。
         */
        private DimensionPolicy ip = defaultIp();

        /**
         * 同一设备指纹的失败保护；默认 5 分钟内 30 次失败后封禁 1800 秒，封禁键类型 {@code DV}。
         */
        private DimensionPolicy device = defaultDevice();

        /**
         * 同一 OAuth2 Client 的失败保护；默认 5 分钟内 100 次失败后封禁 3600 秒，封禁键类型 {@code CL}。
         */
        private DimensionPolicy client = defaultClient();

        /**
         * 同一账号+IP 组合的失败保护；默认 5 分钟内 10 次失败后封禁 3600 秒。
         * <p>计数按账号+IP，封禁键类型仍为 {@code IP}，即封该 IP 而非账号。</p>
         */
        private DimensionPolicy accountIp = defaultAccountIp();
    }

    /**
     * <p>单个登录失败维度的阈值与封禁参数。</p>
     *
     * @author jy
     * @since 1.0.0
     *
     * @see com.ingot.framework.security.access.model.LoginFailurePolicy
     */
    @Data
    public static class DimensionPolicy {
        /**
         * 该维度总开关。关闭后不计失败次数、不写入临时封禁。
         */
        private boolean enabled = true;

        /**
         * 滑动窗口内允许的最大失败次数；达到后写入临时封禁。
         */
        private int maxAttempts = 50;

        /**
         * 失败计数的滑动窗口，单位分钟。
         */
        private int windowMinutes = 1;

        /**
         * 临时封禁存活时间，单位秒。
         */
        private int blockTtlSec = 3600;

        /**
         * 临时封禁 Redis 键类型，写入 {@code in:gw:bl:tmp:{blockKeyType}:{value}}，与网关黑名单共用。
         * <p>常用值：{@code IP}、{@code DV}（设备）、{@code CL}（Client）。</p>
         */
        private String blockKeyType = "IP";
    }

    private static DimensionPolicy defaultIp() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(50);
        p.setWindowMinutes(1);
        p.setBlockTtlSec(3600);
        p.setBlockKeyType("IP");
        return p;
    }

    private static DimensionPolicy defaultDevice() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(30);
        p.setWindowMinutes(5);
        p.setBlockTtlSec(1800);
        p.setBlockKeyType("DV");
        return p;
    }

    private static DimensionPolicy defaultClient() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(100);
        p.setWindowMinutes(5);
        p.setBlockTtlSec(3600);
        p.setBlockKeyType("CL");
        return p;
    }

    private static DimensionPolicy defaultAccountIp() {
        DimensionPolicy p = new DimensionPolicy();
        p.setMaxAttempts(10);
        p.setWindowMinutes(5);
        p.setBlockTtlSec(3600);
        p.setBlockKeyType("IP");
        return p;
    }
}
