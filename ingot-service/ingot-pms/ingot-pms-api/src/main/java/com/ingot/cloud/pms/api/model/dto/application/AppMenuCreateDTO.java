package com.ingot.cloud.pms.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import java.util.List;

import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionMatchModeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>创建应用内菜单请求。受保护页面提交已有权限 ID，不再从路径生成或托管权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "创建应用内菜单")
public class AppMenuCreateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父菜单 ID，根菜单为空或 0")
    private Long pid;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型；Button 不再作为导航节点")
    private MenuTypeEnum menuType;

    @Schema(description = "菜单路径，外部链接可留空自动生成")
    private String path;

    @Schema(description = "访问模式")
    private AccessModeEnum accessMode;

    @Schema(description = "可见性关联的具体权限 ID；受保护页面必填，目录和 OPEN 必须为空")
    private List<Long> permissionIds;

    @Schema(description = "权限匹配模式，缺省 ANY")
    private PermissionMatchModeEnum permissionMatchMode;

    @Schema(description = "页面或布局注册键，与 path 独立；默认链接的目录/菜单必填，原样落库")
    private String viewPath;

    @Schema(description = "命名路由")
    private String routeName;

    @Schema(description = "重定向")
    private String redirect;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否缓存")
    private Boolean isCache;

    @Schema(description = "是否隐藏")
    private Boolean hidden;

    @Schema(description = "是否隐藏面包屑")
    private Boolean hideBreadcrumb;

    @Schema(description = "是否匹配 props")
    private Boolean props;

    @Schema(description = "链接类型")
    private MenuLinkTypeEnum linkType;

    @Schema(description = "链接 URL")
    private String linkUrl;

    @Schema(description = "状态")
    private CommonStatusEnum status;

    @Schema(description = "备注")
    private String remark;
}
