package com.ingot.cloud.pms.api.model.vo.menu;

import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>Description  : MenuTreeNodeVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/29.</p>
 * <p>Time         : 8:31 上午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuTreeNodeVO extends TreeNode<Long> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单名称
     */
    private String name;
    /**
     * 菜单类型
     */
    private MenuTypeEnum menuType;
    /**
     * 菜单url
     */
    private String path;
    /**
     * 是否开启权限
     */
    private Boolean enableAuthority;
    /**
     * 权限ID
     */
    private Long authorityId;
    /**
     * 权限编码
     */
    private String authorityCode;
    /**
     * 是否自定义视图路径
     */
    private Boolean customViewPath;
    /**
     * 视图路径
     */
    private String viewPath;
    /**
     * 命名路由
     */
    private String routeName;
    /**
     * 重定向
     */
    private String redirect;
    /**
     * 图标
     */
    private String icon;
    /**
     * 排序
     */
    private int sort;
    /**
     * 是否缓存
     */
    private Boolean isCache;
    /**
     * 是否隐藏
     */
    private Boolean hidden;
    /**
     * 是否隐藏面包屑
     */
    private Boolean hideBreadcrumb;
    /**
     * 是否匹配props
     */
    private Boolean props;
    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
    /**
     * 菜单组织类型
     */
    private OrgTypeEnum orgType;
    /**
     * 链接类型
     */
    private MenuLinkTypeEnum linkType;
    /**
     * 链接url
     */
    private String linkUrl;
}
