package com.ingot.cloud.security.api.model.vo.policy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import lombok.Data;

/**
 * <p>并发会话策略视图对象，安全中心下发给 Auth 执行面的策略契约。</p>
 *
 * <p>Auth 按 {@link #scope} 由窄到宽选取唯一命中记录，因此这里返回的是策略<b>全量列表</b>而非
 * 单条已解析结果 —— 解析发生在 Auth 侧，安全中心不感知具体登录上下文。
 * {@link #clientId} / {@link #userType} 在不适用的 scope 下为空串，不用 {@code null}，
 * 以便与唯一索引保持一致。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class SessionConcurrencyPolicyVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 生效范围
     */
    private SessionPolicyScope scope;

    /**
     * {@code scope=CLIENT} 时的 OAuth2 Client ID，其余 scope 为空串
     */
    private String clientId;

    /**
     * {@code scope=USER_TYPE} 时的用户类型（{@code UserTypeEnum} 值），其余 scope 为空串
     */
    private String userType;

    /**
     * 最大并发会话数；{@code 0} 表示无限
     */
    private Integer maxSessions;

    /**
     * 会话数统计维度
     */
    private SessionConcurrencyDimension dimension;

    /**
     * 超限处置方式
     */
    private SessionOverflowStrategy overflow;

    /**
     * 管理用户是否强制单会话
     */
    private Boolean adminForbidConcurrent;

    private Boolean enabled;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
