package com.ingot.cloud.pms.api.utils;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ingot.cloud.pms.api.model.base.TreeNode;

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

    /**
     * 展开 tree
     *
     * @param tree 树结构列表
     * @param <T>  类型
     * @return 展开列表
     */
    @SuppressWarnings("unchecked")
    public static <T extends TreeNode> List<T> stretch(List<T> tree) {
        List<T> list = new ArrayList<>();

        for (T node : tree) {
            list.add(node);
            if (!CollUtil.isEmpty(node.getChildren())) {
                list.addAll(stretch((List<T>) node.getChildren()));
                node.setChildren(null);
            }
        }

        return list;
    }

    /**
     * 检索树中是否包含指定节点
     *
     * @param tree   树结构列表
     * @param target 节点
     * @param <T>    类型
     * @return 是否包含
     */
    public static <T extends TreeNode> boolean contains(List<T> tree, T target) {
        for (T node : tree) {
            if (ObjectUtil.equal(node.getId(), target.getId())) {
                return true;
            }
            if (!CollUtil.isEmpty(node.getChildren())
                    && contains(node.getChildren(), target)) {
                return true;
            }
        }

        return false;
    }
}
