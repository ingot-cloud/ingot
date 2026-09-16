package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存平台域静态用户组。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_platform_group", autoResultMap = true)
public class IamPlatformGroupEntity {
    /** 组 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 组名称。 */
    @TableField("name")
    private String name;

    /** 组说明，可空。 */
    @TableField("description")
    private String description;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间，UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
