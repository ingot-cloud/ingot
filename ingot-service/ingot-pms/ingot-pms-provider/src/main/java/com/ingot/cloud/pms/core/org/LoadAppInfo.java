package com.ingot.cloud.pms.core.org;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import lombok.Data;

/**
 * <p>Description  : 装载APP所用信息.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/30.</p>
 * <p>Time         : 18:12.</p>
 */
@Data
public class LoadAppInfo {
    /**
     * 默认租户应用对应的菜单列表
     */
    private List<MenuTreeNodeVO> menuList;
    /**
     * 默认租户应用对应的菜单树结构
     */
    private List<MenuTreeNodeVO> menuTree;
    /**
     * 默认租户应用对应的权限列表平级结构
     */
    private List<AuthorityTreeNodeVO> authorityList;
    /**
     * 默认租户应用对应的权限树结构
     */
    private List<AuthorityTreeNodeVO> authorityTree;
    /**
     * 根权限ID的所有上级权限，包括自己
     */
    private List<SysAuthority> authorityParentTemplateList;
    /**
     * 根菜单ID的所有上级菜单，包括自己
     */
    private List<SysMenu> menuParentTemplateList;
}
