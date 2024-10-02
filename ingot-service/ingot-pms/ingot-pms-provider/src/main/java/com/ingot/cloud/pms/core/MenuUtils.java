package com.ingot.cloud.pms.core;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.UUIDUtils;

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
                // 1.菜单未开启权限的内容
                // 2.菜单开启权限并且拥有该权限，且权限可用
                .filter(node -> node.getEnableAuthority() == null || BooleanUtil.isFalse(node.getEnableAuthority()) ||
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

    /**
     * 设置外部链接path
     */
    public static void setMenuOuterLinkPath(SysMenu params, Long pid, SysMenuService sysMenuService) {
        String pPath = "";
        if (pid != null && pid > 0) {
            SysMenu parent = sysMenuService.getById(pid);
            pPath = parent.getPath() + "/";
        } else {
            pPath = "/";
        }
        String path = pPath + UUIDUtils.generateShortUuid();
        params.setPath(path);
    }

    /**
     * 根据path生成视图路径
     *
     * @param menu {@link SysMenu}
     */
    public static void setViewPathAccordingToPath(SysMenu menu) {
        String path = menu.getPath();
        if (BooleanUtil.isTrue(menu.getProps())) {
            path = StrUtil.subBefore(path, "/", true);
        }
        menu.setViewPath("@/pages" + path + "/IndexPage.vue");
    }

    /**
     * 获取菜单权限code, 将菜单path替换为编码<br>
     * path：/a/b/c => a:b:c
     *
     * @param menu {@link SysMenu}
     * @return code
     */
    public static String getMenuAuthorityCode(SysMenu menu) {
        String path = menu.getPath();
        String r = StrUtil.replace(path, StrUtil.COLON, "");
        r = StrUtil.replace(r, StrUtil.SLASH, StrUtil.COLON);
        if (StrUtil.startWith(r, StrUtil.COLON)) {
            return r.substring(1);
        }
        return r;
    }
}
