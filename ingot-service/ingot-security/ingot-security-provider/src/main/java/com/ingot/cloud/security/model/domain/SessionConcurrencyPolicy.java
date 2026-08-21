package com.ingot.cloud.security.model.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>并发会话策略实体，对应 {@code ingot_security.session_concurrency_policy}。</p>
 *
 * <p>{@link #clientId} 与 {@link #userType} 在不适用的 {@link #scope} 下存空串而非
 * {@code null}，因为唯一索引 {@code uq_session_concurrency_scope} 建立在这三列上，
 * MySQL 的 {@code NULL} 不参与唯一性比较，用 {@code null} 会让同一 scope 出现重复行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("session_concurrency_policy")
public class SessionConcurrencyPolicy implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private SessionPolicyScope scope;

    private String clientId;

    private String userType;

    private Integer maxSessions;

    private SessionConcurrencyDimension dimension;

    private SessionOverflowStrategy overflow;

    private Boolean adminForbidConcurrent;

    private Boolean enabled;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
