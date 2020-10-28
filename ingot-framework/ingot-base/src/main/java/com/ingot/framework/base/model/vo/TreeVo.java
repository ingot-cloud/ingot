package com.ingot.framework.base.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : TreeVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/6.</p>
 * <p>Time         : 1:22 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TreeVo extends BaseVo{
    /**
     * 父ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pid;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 是否叶子节点,1是0不是
     */
    private Integer leaf;
}
