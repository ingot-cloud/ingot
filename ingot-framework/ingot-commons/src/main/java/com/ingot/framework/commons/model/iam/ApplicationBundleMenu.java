package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
 * @param hidden 是否隐藏导航入口
 * @param isCache 是否缓存页面状态
 * @param props 是否将路径参数传给页面
 * @param routeParams 有序路径参数声明，省略时默认为空
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
        int sortOrder,
        @Schema(description = "是否隐藏导航入口")
        Boolean hidden,
        @Schema(description = "是否缓存页面状态")
        Boolean isCache,
        @Schema(description = "是否将路径参数传给页面")
        Boolean props,
        @Schema(description = "有序路径参数声明，省略时默认为空")
        List<@Valid MenuRouteParam> routeParams) {

    /**
     * 保留既有 Java 调用方，新字段使用默认值。
     */
    public ApplicationBundleMenu(
            String tempId,
            String parentTempId,
            String name,
            MenuKind kind,
            String path,
            String viewPath,
            String routeName,
            String icon,
            MenuAccessMode accessMode,
            ActionMatchMode matchMode,
            List<String> actionTempIds,
            int sortOrder) {
        this(tempId, parentTempId, name, kind, path, viewPath, routeName, icon, accessMode, matchMode, actionTempIds, sortOrder, null, null, null, null);
    }

    /**
     * 复制操作临时 ID 列表。
     */
    public ApplicationBundleMenu {
        if (routeParams != null) {
            routeParams = Collections.unmodifiableList(new ArrayList<>(routeParams));
        }
        if (actionTempIds != null) {
            actionTempIds = Collections.unmodifiableList(new ArrayList<>(actionTempIds));
        }
    }
}
