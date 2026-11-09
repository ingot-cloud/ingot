package com.ingot.cloud.iam.infrastructure.persistence.entity;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-10-24
 */
@Getter
@Setter
@TableName("fw_id_leaf_alloc")
public class IdAllocationEntity extends BaseModel<IdAllocationEntity> {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("biz_tag")
    private String bizTag;

    private Long maxId;

    private Integer step;

    @TableField("`description`")
    private String description;

    private LocalDateTime updateTime;
}
