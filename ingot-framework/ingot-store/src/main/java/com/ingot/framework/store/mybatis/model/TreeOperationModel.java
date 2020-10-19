package com.ingot.framework.store.mybatis.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : TreeOperationModel.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 5:03 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TreeOperationModel<T extends Model<?>> extends OperationModel<T> {
    /**
     * ID
     */
    @TableId
    private String id;

    /**
     * 父ID
     */
    private String pid;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 是否叶子节点
     */
    private Boolean isLeaf;
}
