package com.ingot.cloud.member.common;

import java.util.List;
import java.util.stream.Collectors;

import com.ingot.cloud.member.api.model.convert.MemberPermissionConvert;
import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.framework.commons.utils.tree.TreeUtil;

/**
 * <p>Description  : BizUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/14.</p>
 * <p>Time         : 10:38 AM.</p>
 */
public class BizUtils {
    /**
     * 权限转为tree结构
     *
     * @param authorities {@link MemberPermission}
     * @param condition   条件
     * @return {@link MemberPermissionTreeNodeVO}
     */
    public static List<MemberPermissionTreeNodeVO> mapTree(List<MemberPermission> authorities,
                                                           MemberPermission condition) {
        List<MemberPermissionTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(MemberPermissionConvert.INSTANCE::toTreeNode).collect(Collectors.toList());

        List<MemberPermissionTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }
}
