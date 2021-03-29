package com.ingot.cloud.pms.api.model.base;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : TreeNode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 10:37 下午.</p>
 */
@Data
public class TreeNode implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 子节点
     */
    private List<TreeNode> children;

    public void add(TreeNode node) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(node);
    }
}
