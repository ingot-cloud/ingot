package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射操作目录的持久化字段。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_action", autoResultMap = true)
public class IamActionEntity {
    /** 操作 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 所属资源 ID。 */
    @TableField("resource_id")
    private BigInteger resourceId;

    /** 精确操作编码。 */
    @TableField("code")
    private String code;

    /** 操作名称。 */
    @TableField("name")
    private String name;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

}
