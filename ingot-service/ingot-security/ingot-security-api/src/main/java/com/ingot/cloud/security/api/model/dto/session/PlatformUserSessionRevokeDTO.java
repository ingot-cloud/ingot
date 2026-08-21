package com.ingot.cloud.security.api.model.dto.session;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * <p>安全中心按用户强制下线的请求参数，以 query 参数绑定。</p>
 *
 * <p>撤销原因固定为管理员撤销，操作者取当前登录管理员，不接受前端传入，
 * 避免安全事件的 {@code operatorId} 与 {@code reason} 被伪造。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote {@code clientId} 留空表示下线该用户在该租户下全部 Client 的会话。
 */
@Data
public class PlatformUserSessionRevokeDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID，为空取当前租户上下文。
     */
    private Long tenantId;

    /**
     * 目标用户 ID，必填。
     */
    private Long userId;

    /**
     * 限定 Client，为空表示全部 Client。
     */
    private String clientId;
}
