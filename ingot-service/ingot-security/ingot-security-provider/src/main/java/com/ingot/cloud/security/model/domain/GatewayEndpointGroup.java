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
 * API 路径分组实体。
 *
 * <p>映射 DB 表 {@code gateway_endpoint_group}，将一组 HTTP 路径模式
 * 抽象为可复用分组，供限流规则、挑战策略等通过 {@code groupCode} 引用。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "gateway_endpoint_group", autoResultMap = true)
public class GatewayEndpointGroup implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，雪花算法分配。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分组编码，全局唯一（{@code uq_endpoint_group_code}）。
     */
    private String code;

    /**
     * 分组显示名称。
     */
    private String name;

    /**
     * 本分组包含的 API 路径匹配列表，JSON 序列化存储，元素类型为 {@link EndpointPatternVO}。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<EndpointPatternVO> patternList;

    /**
     * 是否启用。
     */
    private Boolean enabled;

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
