package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.cloud.security.api.model.vo.policy.EndpointPatternVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 挑战策略实体。
 *
 * <p>映射 DB 表 {@code security_challenge_policy}（Phase 4），
 * 定义在特定 API 路径上何时触发验证码挑战及 PassToken 发放规则。
 * 登录失败锁定由 account-domain 处理，不在本表承载。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "security_challenge_policy", autoResultMap = true)
public class SecurityChallengePolicy implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，雪花算法分配。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 策略编码，全局唯一（{@code uq_challenge_policy_code}）。
     */
    private String code;

    /**
     * 关联的 API 路径分组编码（{@code gateway_endpoint_group.code}）。
     * 非空时优先使用分组路径；为空则使用 {@link #patternList} 内联路径。
     */
    private String groupCode;

    /**
     * 内联 API 路径匹配列表，JSON 序列化存储，元素类型为 {@link EndpointPatternVO}。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<EndpointPatternVO> patternList;

    /**
     * 挑战触发条件：{@code always}（始终挑战）/ {@code on_rate_limit}（限流触发后挑战）。
     * 登录失败锁定由 account-domain 处理，不支持 {@code on_failure_threshold}。
     */
    @TableField("`trigger`")
    private String trigger;

    /**
     * 验证码挑战类型：{@code SLIDER}（滑块）/ {@code IMAGE}（图形）/ {@code SMS}（短信）/ {@code EMAIL}（邮件）。
     */
    private String challengeType;

    /**
     * 已废弃字段，保留列兼容；网关执行面不读取。
     */
    private String failureDimension;

    /**
     * 已废弃字段，保留列兼容；网关执行面不读取。
     */
    private Integer failureThreshold;

    /**
     * 已废弃字段，保留列兼容；网关执行面不读取。
     */
    private Integer failureWindowSec;

    /**
     * PassToken 有效期（秒），验证通过后颁发的通行令牌存活时间。
     */
    private Integer passTokenTtlSec;

    /**
     * PassToken 可消费次数，令牌在有效期内允许通过的请求次数。
     */
    private Integer passTokenRemaining;

    /**
     * 管理面保留字段：验码连续失败拉黑阈值；网关 Phase 1 未实现验码失败拉黑
     * （临时封禁由限流违规计数触发）。
     */
    private Integer challengeFailureLimit;

    /**
     * 管理面保留字段：封禁时长（秒）；网关限流违规封禁时长见 {@code SentinelBlockHandler} 常量。
     */
    private Integer blockTtlSec;

    /**
     * 策略作用域，与 PassToken scope 对齐，用于隔离不同业务场景的通行凭证。
     */
    private String scope;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 匹配优先级，数值越小越优先。
     */
    private Integer priority;

    /**
     * 备注说明。
     */
    private String remark;

    /**
     * 记录创建时间，插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间，插入与更新时自动填充。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
