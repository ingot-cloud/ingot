package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>整包创建时的菜单，父级与关联操作用本次请求的临时 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param tempId 客户端临时 ID
 * @param parentTempId 父菜单临时 ID，根节点为空
 * @param name 菜单名称
 * @param kind 目录或页面
 * @param path 路由路径，可空
 * @param viewPath 视图注册键，可空
 * @param routeName 路由名称，可空
 * @param icon 图标，可空
 * @param accessMode 导航准入方式
 * @param matchMode 操作匹配方式
 * @param actionTempIds 关联操作的临时 ID
 * @param sortOrder 展示顺序
 */
@Schema(description = "整包创建时的菜单，父级与关联操作用本次请求的临时 ID")
public record ApplicationBundleMenu(
        @NotBlank @Size(max = 64) @Schema(description = "客户端临时 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String tempId,
        @Schema(description = "父菜单临时 ID，根节点为空")
        String parentTempId,
        @NotBlank @Size(max = 128) @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "目录或页面", requiredMode = Schema.RequiredMode.REQUIRED)
        MenuKind kind,
        @Schema(description = "路由路径，可空")
        String path,
        @Schema(description = "视图注册键，可空")
        String viewPath,
        @Schema(description = "路由名称，可空")
        String routeName,
        @Schema(description = "图标，可空")
        String icon,
        @NotNull @Schema(description = "导航准入方式", requiredMode = Schema.RequiredMode.REQUIRED)
        MenuAccessMode accessMode,
        @NotNull @Schema(description = "操作匹配方式", requiredMode = Schema.RequiredMode.REQUIRED)
        ActionMatchMode matchMode,
        @NotNull @Schema(description = "关联操作的临时 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> actionTempIds,
        @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder) {

    /**
     * 复制操作临时 ID 列表。
     */
    public ApplicationBundleMenu {
        if (actionTempIds != null) {
            actionTempIds = Collections.unmodifiableList(new ArrayList<>(actionTempIds));
        }
    }
}
