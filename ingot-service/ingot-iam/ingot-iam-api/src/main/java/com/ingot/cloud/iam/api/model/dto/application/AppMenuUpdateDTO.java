package com.ingot.cloud.iam.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import java.util.List;

import com.ingot.cloud.iam.api.model.enums.AccessModeEnum;
import com.ingot.cloud.iam.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.iam.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.iam.api.model.enums.PermissionMatchModeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>更新应用内菜单请求，仅覆盖显式传入的非空字段。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "更新应用内菜单")
public class AppMenuUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型")
    private MenuTypeEnum menuType;

    @Schema(description = "菜单路径")
    private String path;

    @Schema(description = "访问模式")
    private AccessModeEnum accessMode;

    @Schema(description = "可见性关联的具体权限 ID；传入则整体替换")
    private List<Long> permissionIds;

    @Schema(description = "权限匹配模式")
    private PermissionMatchModeEnum permissionMatchMode;

    @Schema(description = "页面或布局注册键，与 path 独立；传入则原样覆盖，不随 path 重算")
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
