package com.ingot.cloud.iam.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>应用内业务资源目录，作为数据范围规则的登记对象。</p>
 *
 * <p>资源编码在应用内唯一；未登记资源不自动获得行级过滤。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@TableName("platform_resource")
public class PlatformResource extends BaseModel<PlatformResource> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属应用
     */
    private Long appId;

    /**
     * 应用内稳定资源编码，创建后不可通过普通更新修改
     */
    @TableField("`code`")
    private String code;

    /**
     * 资源名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 状态，{@link CommonStatusEnum#ENABLE} 可用
     */
    @TableField("`status`")
    private CommonStatusEnum status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
