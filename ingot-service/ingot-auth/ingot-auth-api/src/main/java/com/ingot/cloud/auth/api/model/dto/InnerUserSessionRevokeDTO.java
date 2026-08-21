package com.ingot.cloud.auth.api.model.dto;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>按用户批量撤销会话的请求体。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote {@code clientId} 为空表示撤销该用户在当前租户下全部 Client 的会话，
 * 账号域改密、锁定等联动场景应当留空以覆盖全部登录入口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerUserSessionRevokeDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private Long userId;

    /**
     * 限定 Client，为空表示当前租户下全部 Client。
     */
    private String clientId;

    /**
     * 撤销原因，为空按 {@link SessionRevokeReason#ADMIN_REVOKE} 处理。
     */
    private SessionRevokeReason reason;

    /**
     * 操作者用户 ID，写入安全事件 {@code operatorId}；系统自动触发时为空。
     */
    private Long actorId;
}
