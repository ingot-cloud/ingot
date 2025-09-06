package com.ingot.framework.commons.utils.tree;

import java.util.List;

/**
 * <p>Description  : TreeModel.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/3/22.</p>
 * <p>Time         : 10:05.</p>
 */
public interface TreeModel<T> {

    /**
     * ID
     *
     * @return ID
     */
    T getId();

    /**
     * Pid
     *
     * @return Pid
     */
    T getPid();

    /**
     * Children
     *
     * @return 孩子节点
     */
    List<? extends TreeModel<T>> getChildren();

    /**
     * 添加子
     *
     * @param child model
     */
    void add(TreeModel<T> child);
}
