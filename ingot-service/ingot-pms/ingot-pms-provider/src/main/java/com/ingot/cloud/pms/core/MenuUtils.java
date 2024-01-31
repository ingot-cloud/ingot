package com.ingot.cloud.pms.core;

import cn.hutool.core.util.ObjectUtil;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.enums.CommonStatusEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Description  : MenuUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/23.</p>
 * <p>Time         : 17:01.</p>
 */
public class MenuUtils {

    /**
     * 根据权限筛选过滤菜单
     *
     * @param allNodeList 所有菜单节点
     * @param authorities 权限列表
     * @return {@link MenuTreeNodeVO}
     */
    public static List<MenuTreeNodeVO> filterMenus(List<MenuTreeNodeVO> allNodeList,
                                                   List<? extends AuthorityType> authorities) {

        List<MenuTreeNodeVO> nodeList = allNodeList.stream()
                // 1.菜单未绑定权限，直接过滤通过
                // 2.菜单绑定权限，并且绑定的权限可用
                .filter(node -> node.getAuthorityId() == null || node.getAuthorityId() == 0 ||
                        authorities.stream()
                                .anyMatch(authority ->
                                        node.getAuthorityId().equals(authority.getId())
                                                && authority.getStatus() == CommonStatusEnum.ENABLE))
                .filter(node -> node.getStatus() == CommonStatusEnum.ENABLE)
                .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort))
                .collect(Collectors.toList());

        // 如果过滤后的列表中存在父节点，并且不在当前列表中，那么需要增加
        // 如果父节点被禁用，那么所有子节点都不可用
        List<MenuTreeNodeVO> copy = new ArrayList<>(nodeList);
        copy.stream()
                .filter(node -> node.getPid() != IDConstants.ROOT_TREE_ID)
                .forEach(node -> {
                    if (nodeList.stream().noneMatch(item -> ObjectUtil.equals(item.getId(), node.getPid()))) {
                        allNodeList.stream()
                                .filter(item -> ObjectUtil.equals(item.getId(), node.getPid()))
                                .findFirst()
                                .ifPresent(nodeList::add);
                    }
                });

        return nodeList;
    }
}
