package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射应用套餐的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_plan", autoResultMap = true)
public class IamPlanEntity {
    /** 记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 名称。 */
    @TableField("name")
    private String name;

    /** 说明。 */
    @TableField("description")
    private String description;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

}
