package com.ingot.cloud.auth.api.model.dto;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>按会话 ID 撤销的请求体，会话 ID 走路径参数。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerSessionRevokeDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 撤销原因，决定安全事件类型与审计口径；为空按 {@link SessionRevokeReason#ADMIN_REVOKE} 处理。
     */
    private SessionRevokeReason reason;

    /**
     * 操作者用户 ID，写入安全事件 {@code operatorId}；系统自动触发时为空。
     */
    private Long actorId;
}
