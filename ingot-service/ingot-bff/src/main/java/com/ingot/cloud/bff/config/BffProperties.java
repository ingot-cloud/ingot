package com.ingot.cloud.bff.config;

import java.util.ArrayList;
import java.util.List;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffFingerprintMode;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 同源内部登录配置；注册表仅 BFF/Gateway 消费，TTL 单位为秒。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ingot.bff", ignoreUnknownFields = false)
public class BffProperties {

    /** 预授权的管理用户类型，默认管理用户。 */
    private String userType = UserTypeEnum.ADMIN.getValue();

    /** 正式会话及 Cookie 寿命秒数，默认 7200。 */
    private long sessionTtl = 7200;

    /** 用户点击后新建事务的硬寿命秒数，默认 600，不滑动续期。 */
    private long transactionTtl = 600;

    /** 完成结果仅用于原绑定恢复的窗口秒数，默认 60，不超过正式会话寿命。 */
    private long completionRecoveryTtl = 60;

    /** 每绑定未终结事务上限，默认 5。 */
    private int maxActiveTransactions = 5;

    /** 单次认证的执行租约秒数，默认 30；超时不接管重做 Auth。 */
    private long executionLeaseSeconds = 30;

    /** 后台清理间隔毫秒，默认 5000。 */
    private long cleanupInterval = 5000;

    /** 生产必须 HTTPS；false 仅限 loopback 开发。 */
    private boolean requireHttps = true;

    /** 两个固定应用的同源注册表。 */
    private List<BffAppRegistration> apps = new ArrayList<>();

    /** 附加设备指纹校验配置，不替代绑定及 CSRF。 */
    private SecurityConfig security = new SecurityConfig();

    /** 拒绝使安全期限失效的配置。 */
    @PostConstruct
    public void validate() {
        if (transactionTtl <= 0 || sessionTtl <= 0 || completionRecoveryTtl <= 0 || completionRecoveryTtl > sessionTtl
                || maxActiveTransactions <= 0 || executionLeaseSeconds <= 0 || cleanupInterval <= 0)
            throw new IllegalStateException("BFF 期限、容量和清理间隔必须为正，恢复窗口不能超过会话寿命");
    }

    /**
     * <p>
     * 设备指纹来源及开关。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class SecurityConfig {

        /** 默认启用；关闭后不保存指纹。 */
        private boolean fingerprintEnabled = true;

        /** 默认 device；ip_ua 只使用可信代理 IP 与 User-Agent。 */
        private BffFingerprintMode fingerprintMode = BffFingerprintMode.DEVICE;

    }

}
