package com.ingot.cloud.pms.api.model.vo.application;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>应用详情视图，含根权限信息与菜单/权限资源统计。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "应用详情")
public class AppDetailVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "应用 ID")
    private Long id;

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "应用类型（组织维度）")
    private OrgTypeEnum appType;

    @Schema(description = "应用图标")
    private String icon;

    @Schema(description = "应用介绍")
    private String intro;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private CommonStatusEnum status;

    @Schema(description = "根权限 ID")
    private Long rootPermissionId;

    @Schema(description = "根权限编码，形如 appCode:**")
    private String rootPermissionCode;

    @Schema(description = "菜单数量")
    private Long menuCount;

    @Schema(description = "权限数量")
    private Long permissionCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
