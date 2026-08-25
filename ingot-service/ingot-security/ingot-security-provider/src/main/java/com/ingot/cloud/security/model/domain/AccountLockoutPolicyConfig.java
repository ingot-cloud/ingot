package com.ingot.cloud.security.model.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>账号登录失败锁定策略实体，对应 {@code account_lockout_policy_config}。</p>
 *
 * <p>按 {@link UserTypeEnum} 分行；管理面只允许更新，不允许删除。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("account_lockout_policy_config")
@Schema(description = "账号登录失败锁定策略")
public class AccountLockoutPolicyConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    /**
     * 用户类型：{@code 0} ADMIN、{@code 1} APP。
     */
    @Schema(description = "用户类型：0=B端管理员，1=C端用户", allowableValues = {"0", "1"})
    private UserTypeEnum userType;

    /**
     * 是否启用自动锁定。
     */
    @Schema(description = "是否启用自动锁定")
    private Boolean enabled;

    /**
     * 失败次数阈值，须 ≥ 1。
     */
    @Schema(description = "失败次数阈值，须 ≥ 1")
    private Integer maxAttempts;

    /**
     * 锁定时长（分钟）。{@code 0} 表示永久锁定，仅 ADMIN 允许。
     */
    @Schema(description = "锁定时长（分钟），0=永久（仅 B 端允许）")
    private Integer lockDurationMinutes;

    /**
     * 失败计数窗口（分钟），须 ≥ 1。
     */
    @Schema(description = "失败计数窗口（分钟），须 ≥ 1")
    private Integer attemptWindowMinutes;

    /**
     * 从第几次失败开始给出剩余次数提示。
     */
    @Schema(description = "从第几次失败开始给出剩余次数提示")
    private Integer hintAfterAttempts;

    /**
     * 备注。
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
