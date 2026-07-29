package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 记录登录用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface RecordLoginUseCase {

    /**
     * 记录登录成功
     *
     * @param command 登录命令
     */
    void recordSuccess(LoginCommand command);

    /**
     * 记录登录失败（会触发失败策略）
     *
     * @param command 登录命令
     */
    void recordFailure(LoginCommand command);

    /**
     * 登录命令
     */
    @Value
    @Builder
    class LoginCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 用户名
         */
        String username;

        /**
         * 客户端IP
         */
        String clientIp;

        /**
         * 客户端信息（User-Agent）
         */
        String userAgent;

        /**
         * 租户ID（来自上下文）
         */
        Long tenantId;

        /**
         * 失败原因（登录失败时填写）
         */
        String failureReason;
    }
}
