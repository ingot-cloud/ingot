package com.ingot.cloud.pms.api.model.vo.menu;

import java.io.Serial;

import java.util.List;

import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionMatchModeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : MenuTreeNodeVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/29.</p>
 * <p>Time         : 8:31 上午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuTreeNodeVO extends TreeNode<Long, MenuTreeNodeVO> {
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
     * 访问模式
     */
    private AccessModeEnum accessMode;
    /**
     * 可见性关联的具体权限 ID
     */
    private List<Long> permissionIds;
    /**
     * 权限匹配模式
     */
    private PermissionMatchModeEnum permissionMatchMode;
    /**
     * 前端页面或布局注册键，与 path 独立
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
    /**
     * 所属应用 ID
     */
    private Long appId;
    /**
     * 应用编码
     */
    private String appCode;
    /**
     * 备注
     */
    private String remark;
}
