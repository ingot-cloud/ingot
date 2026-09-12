package com.ingot.cloud.pms.api.model.dto.authorization;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * <p>内部授权快照请求。身份以认证上下文为准；若携带租户或用户且与上下文不一致则拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class AuthorizationSnapshotRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 可选租户 ID；非空时必须与认证上下文一致。
     */
    private Long tenantId;

    /**
     * 可选用户 ID；非空时必须与认证上下文一致。
     */
    private Long userId;
}
