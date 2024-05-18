package com.ingot.cloud.pms.core.org;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import lombok.Data;

import java.util.List;

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
     * 默认租户应用对应的权限列表
     */
    private List<AuthorityTreeNodeVO> authorityTree;
    /**
     * 默认租户应用对应权限的根权限ID
     */
    private long rootAuthorityParentId;
    /**
     * 默认租户应用对应菜单的根目录ID
     */
    private long rootMenuParentId;
    /**
     * 根权限ID的所有上级权限，包括自己
     */
    private List<SysAuthority> authorityParentTemplateList;
    /**
     * 根菜单ID的所有上级菜单，包括自己
     */
    private List<SysMenu> menuParentTemplateList;
}
