package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
@TableName("biz_leaf_alloc")
public class BizLeafAlloc extends BaseModel<BizLeafAlloc> {

    private static final long serialVersionUID = 1L;

    @TableId("biz_tag")
    private String bizTag;

    private Long maxId;

    private Integer step;

    @TableField("`description`")
    private String description;

    private LocalDateTime updateTime;
}
