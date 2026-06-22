package com.ingot.cloud.pms.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
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

    @Schema(description = "是否自定义视图路径")
    private Boolean customViewPath;

    @Schema(description = "视图路径")
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
}
