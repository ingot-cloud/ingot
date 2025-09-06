package com.ingot.framework.commons.utils.tree;

import java.util.List;
import java.util.Optional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.lang.tree.parser.DefaultNodeParser;
import cn.hutool.core.util.ObjectUtil;
import com.ingot.framework.commons.constants.IDConstants;

/**
 * <p>Description  : TreeUtilHelper.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/26.</p>
 * <p>Time         : 11:55 AM.</p>
 */
public class TreeUtilHelper {

    private static final TreeNodeConfig DEFAULT_CONFIG = new TreeNodeConfig() {
        @Override
        public String getParentIdKey() {
            return "pid";
        }
    };

    /**
     * 构建树形结构列表
     *
     * @param list 树节点列表
     * @return {@link Tree} list
     */
    public static List<Tree<Long>> build(List<TreeNode<Long>> list) {
        return build(list, IDConstants.ROOT_TREE_ID);
    }

    /**
     * 构建树形结构列表
     *
     * @param list     树节点列表
     * @param parentId 父id
     * @param <T>      id类型
     * @return {@link Tree} list
     */
    public static <T> List<Tree<T>> build(List<TreeNode<T>> list, T parentId) {
        return Optional.ofNullable(TreeUtil.build(list, parentId, DEFAULT_CONFIG, new DefaultNodeParser<>()))
                .orElse(CollUtil.empty(List.class));
    }

    /**
     * 构建树
     *
     * @param list     树节点列表
     * @param parentId 父id
     * @param <T>      id类型
     * @return {@link Tree}
     */
    public static <T> Tree<T> buildSingle(List<TreeNode<T>> list, T parentId) {
        return TreeUtil.buildSingle(list, parentId, DEFAULT_CONFIG, new DefaultNodeParser<>());
    }

    /**
     * 构建树形结构列表
     *
     * @param list     树节点列表
     * @param parentId 父id
     * @param config   配置
     * @param <T>      id类型
     * @return {@link Tree} list
     */
    public static <T> List<Tree<T>> build(List<TreeNode<T>> list, T parentId, TreeNodeConfig config) {
        return Optional.ofNullable(TreeUtil.build(list, parentId, config, new DefaultNodeParser<>()))
                .orElse(CollUtil.empty(List.class));
    }

    /**
     * 是否包含某节点ID
     *
     * @param trees 树形列表
     * @param id    ID
     * @param <T>   ID类型
     * @return boolean是否包含
     */
    public static <T> boolean contains(List<Tree<T>> trees, T id) {
        return trees.stream().anyMatch((node) ->
                ObjectUtil.equal(node.getId(), id)
                        || (!CollUtil.isEmpty(node.getChildren()) && contains(node.getChildren(), id)));
    }

}
