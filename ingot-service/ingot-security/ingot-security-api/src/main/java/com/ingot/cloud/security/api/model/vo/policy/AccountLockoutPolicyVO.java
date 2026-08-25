package com.ingot.cloud.security.api.model.vo.policy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Data;

/**
 * <p>账号登录失败锁定策略视图，供 Inner Feign 与分层缓存反序列化。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class AccountLockoutPolicyVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    private Long id;

    /**
     * 用户类型：{@link UserTypeEnum#ADMIN}（{@code 0}）或 {@link UserTypeEnum#APP}（{@code 1}）。
     */
    private UserTypeEnum userType;

    /**
     * 是否启用自动锁定。
     */
    private Boolean enabled;

    /**
     * 失败次数阈值。
     */
    private Integer maxAttempts;

    /**
     * 锁定时长（分钟），{@code 0} 表示永久锁定。
     */
    private Integer lockDurationMinutes;

    /**
     * 失败计数窗口（分钟）。
     */
    private Integer attemptWindowMinutes;

    /**
     * 从第几次失败开始给出剩余次数提示。
     */
    private Integer hintAfterAttempts;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
