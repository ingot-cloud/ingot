package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回应用菜单配置及独立的操作可见性关联。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 菜单 ID
 * @param applicationId 应用 ID
 * @param parentId 父菜单 ID，根节点为空
 * @param name 菜单名称
 * @param kind 目录或页面
 * @param path 路由路径，可空
 * @param viewPath 视图注册键，可空
 * @param routeName 路由名称，可空
 * @param icon 图标，可空
 * @param accessMode 导航准入方式
 * @param matchMode 操作匹配方式
 * @param actionIds 关联的本应用精确操作 ID
 * @param sortOrder 展示顺序
 * @param status 菜单状态
 * @param hidden 是否隐藏导航入口
 * @param isCache 是否缓存页面状态
 * @param props 是否将路径参数传给页面
 * @param routeParams 有序路径参数声明，默认空数组
 * @param resolvedPath 完整路由模板，仅由服务端生成
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回应用菜单配置及独立的操作可见性关联")
public record MenuRecord(
        @NotBlank @Schema(description = "菜单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @Schema(description = "父菜单 ID，根节点为空")
        String parentId,
        @NotBlank @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
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
        @NotNull @Schema(description = "关联的本应用精确操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> actionIds,
        @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
        @NotNull @Schema(description = "菜单状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status,
        @Schema(description = "是否隐藏导航入口", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hidden,
        @Schema(description = "是否缓存页面状态", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean isCache,
        @Schema(description = "是否将路径参数传给页面", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean props,
        @NotNull @Schema(description = "有序路径参数声明，默认空数组", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@Valid MenuRouteParam> routeParams,
        @Schema(description = "完整路由模板，仅由服务端生成", accessMode = Schema.AccessMode.READ_ONLY)
        String resolvedPath) {

    /**
     * 保留既有 Java 调用方，新字段使用默认值。
     */
    public MenuRecord(
            String id,
            String applicationId,
            String parentId,
            String name,
            MenuKind kind,
            String path,
            String viewPath,
            String routeName,
            String icon,
            MenuAccessMode accessMode,
            ActionMatchMode matchMode,
            List<String> actionIds,
            int sortOrder,
            ConfigurationStatus status) {
        this(id, applicationId, parentId, name, kind, path, viewPath, routeName, icon, accessMode, matchMode, actionIds, sortOrder, status, false, false, false, List.of(), path);
    }

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public MenuRecord {
        routeParams = routeParams == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(routeParams));
        if (actionIds != null) {
            actionIds = Collections.unmodifiableList(new ArrayList<>(actionIds));
        }
    }
}
