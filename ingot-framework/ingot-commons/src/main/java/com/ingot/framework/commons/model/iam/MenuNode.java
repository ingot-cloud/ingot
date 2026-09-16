package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回经同一授权视图筛选后的导航节点，不包含按钮权限伪菜单。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 菜单 ID
 * @param applicationId 所属应用 ID
 * @param name 导航名称
 * @param kind 目录或页面
 * @param path 页面路由，可空
 * @param viewPath 视图注册键，可空
 * @param routeName 路由名称，可空
 * @param icon 图标引用，可空
 * @param sortOrder 展示顺序
 * @param children 可见子节点，叶节点为空数组
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回经同一授权视图筛选后的导航节点，不包含按钮权限伪菜单")
public record MenuNode(
        @NotBlank @Schema(description = "菜单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "所属应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotBlank @Schema(description = "导航名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "目录或页面", requiredMode = Schema.RequiredMode.REQUIRED)
        MenuKind kind,
         @Schema(description = "页面路由，可空")
        String path,
         @Schema(description = "视图注册键，可空")
        String viewPath,
         @Schema(description = "路由名称，可空")
        String routeName,
         @Schema(description = "图标引用，可空")
        String icon,
         @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
        @NotNull @Schema(description = "可见子节点，叶节点为空数组", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid MenuNode> children) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public MenuNode {
        if (children != null) {
            children = Collections.unmodifiableList(new ArrayList<>(children));
        }
    }
}
