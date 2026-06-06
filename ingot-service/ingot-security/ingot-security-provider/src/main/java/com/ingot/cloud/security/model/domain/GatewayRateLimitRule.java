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
 * 网关限流规则实体。
 *
 * <p>映射 DB 表 {@code gateway_rate_limit_rule}，管理面 CRUD 后通过
 * 策略快照下发至网关，由 SDK 编译为 Sentinel 参数流控规则。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "gateway_rate_limit_rule", autoResultMap = true)
public class GatewayRateLimitRule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，雪花算法分配。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 规则编码，全局唯一（{@code uq_rate_limit_rule_code}）。
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
     * 限流维度短码（DB {@code char(2)}）：{@code IP} / {@code DV}(device) / {@code UI}(user)。
     * SDK 通过 {@code RateLimitDimension.fromCode} 解析。
     */
    private String dimension;

    /**
     * 平均请求速率上限（次/秒）。
     */
    private Integer qps;

    /**
     * 突发容量（令牌桶大小）。
     */
    private Integer burst;

    /**
     * 统计窗口长度（秒）。
     */
    private Integer intervalSec;

    /**
     * 流控行为：{@code F}=快速失败 / {@code Q}=排队等待。
     */
    private String controlBehavior;

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
