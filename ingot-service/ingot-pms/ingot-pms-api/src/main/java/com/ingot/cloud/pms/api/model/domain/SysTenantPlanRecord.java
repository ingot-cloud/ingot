package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("sys_tenant_plan_record")
public class SysTenantPlanRecord extends BaseModel<SysTenantPlanRecord> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 计划ID
     */
    private Integer planId;

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
}
