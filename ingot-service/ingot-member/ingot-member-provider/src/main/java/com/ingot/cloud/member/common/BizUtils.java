package com.ingot.cloud.member.common;

import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.member.api.model.convert.MemberPermissionConvert;
import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
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

    /**
     * 根据当前用户状态和可访问租户列表，返回用户最终状态
     */
    public static UserStatusEnum getUserStatus(List<TenantMainDTO> allows, UserStatusEnum userStatus, Long loginTenant) {
        // 没有允许访问的租户，那么直接返回不可用
        if (CollUtil.isEmpty(allows)) {
            return UserStatusEnum.LOCK;
        }
        // 如果允许访问的tenant中不存在当前登录的tenant，那么直接返回不可用
        if (loginTenant != null && allows.stream().noneMatch(item -> Long.parseLong(item.getId()) == loginTenant)) {
            return UserStatusEnum.LOCK;
        }
        return userStatus;
    }
}
