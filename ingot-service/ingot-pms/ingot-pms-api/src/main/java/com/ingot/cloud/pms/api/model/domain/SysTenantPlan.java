package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.cloud.pms.api.model.enums.OrgPlanTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgPlanUnitEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 
 * </p>
 *
 * @author jymot
 * @since 2025-11-11
 */
@Getter
@Setter
@ToString
@TableName("sys_tenant_plan")
public class SysTenantPlan extends BaseModel<SysTenantPlan> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 计划名字
     */
    @TableField("`name`")
    private String name;

    /**
     * 计划类型
     */
    @TableField("`type`")
    private OrgPlanTypeEnum type;

    /**
     * 持续时间
     */
    private Integer duration;

    /**
     * 单位
     */
    private OrgPlanUnitEnum unit;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
