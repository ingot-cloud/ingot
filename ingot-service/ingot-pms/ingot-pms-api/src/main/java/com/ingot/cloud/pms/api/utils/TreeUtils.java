package com.ingot.cloud.pms.api.utils;

import cn.hutool.core.util.ObjectUtil;
import com.ingot.cloud.pms.api.model.base.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : TreeUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/29.</p>
 * <p>Time         : 9:29 下午.</p>
 */
public class TreeUtils {

    /**
     * 构建Tree
     *
     * @param all    所有节点
     * @param rootId 根节点ID
     * @param <T>    子类型
     * @return 树节点列表
     */
    public static <T extends TreeNode> List<T> build(List<T> all, Object rootId) {
        List<T> trees = new ArrayList<>();

        for (T node : all) {
            if (node.getPid() == null ||
                    ObjectUtil.equal(node.getPid(), rootId)) {
                trees.add(node);
            }

            for (T child : all) {
                if (node.getId().equals(child.getPid())) {
                    node.add(child);
                }
            }

        }

        return trees;
    }
}
