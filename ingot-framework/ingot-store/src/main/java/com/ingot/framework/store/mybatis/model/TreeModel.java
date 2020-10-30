package com.ingot.framework.store.mybatis.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : TreeModel.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 4:58 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TreeModel<T extends Model<?>> extends Model<T> {
    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 是否叶子节点
     */
    private Boolean isLeaf;
}
