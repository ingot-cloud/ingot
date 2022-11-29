package com.ingot.cloud.pms.api.model.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : TreeNode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 10:37 下午.</p>
 */
@Data
public class TreeNode<IdType> implements Serializable {
    /**
     * ID
     */
    private IdType id;

    /**
     * 父ID
     */
    private IdType pid;

    /**
     * 子节点
     */
    private List<TreeNode<IdType>> children;

    /**
     * 添加子节点
     *
     * @param node {@link TreeNode}
     */
    public void add(TreeNode<IdType> node) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(node);
    }
}
