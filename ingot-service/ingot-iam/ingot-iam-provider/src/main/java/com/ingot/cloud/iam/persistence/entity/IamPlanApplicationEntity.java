package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射套餐应用关联的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_plan_application", autoResultMap = true)
public class IamPlanApplicationEntity {
    /** 套餐 ID。 */
    @TableField("plan_id")
    private BigInteger planId;

    /** 应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

}
