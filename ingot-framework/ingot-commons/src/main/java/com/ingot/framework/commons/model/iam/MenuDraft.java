package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>提交应用菜单及关联操作，按钮权限仍由独立操作目录表达。</p>
 *
 * @author jy
 * @since 1.0.0
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
 */
@Schema(description = "提交应用菜单及关联操作，按钮权限仍由独立操作目录表达")
public record MenuDraft(
        @Schema(description = "父菜单 ID，根节点为空")
        String parentId,
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
        @NotNull @Schema(description = "关联的本应用精确操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> actionIds,
        @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder) {

    /**
     * 复制操作关联。
     */
    public MenuDraft {
        if (actionIds != null) {
            actionIds = Collections.unmodifiableList(new ArrayList<>(actionIds));
        }
    }
}
