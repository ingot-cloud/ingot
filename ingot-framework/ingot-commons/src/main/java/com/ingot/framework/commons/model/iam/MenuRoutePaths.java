package com.ingot.framework.commons.model.iam;

import java.util.List;

/**
 * <p>根据基础路径和有序参数声明生成路由模板，空声明保留存量路径。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MenuRoutePaths {
    /** 自动路由名称前缀，与管理台约定一致。 */
    public static final String NAME_PREFIX = "iam-menu-";
    private static final String PARAMETER_PREFIX = "/:";

    private MenuRoutePaths() {
    }

    /**
     * 生成完整模板，不改变未声明结构化参数的存量路径。
     * @param path 基础路径
     * @param parameters 有序参数声明
     * @return 完整模板
     */
    public static String resolve(String path, List<MenuRouteParam> parameters) {
        if (path == null || parameters == null || parameters.isEmpty()) {
            return path;
        }
        StringBuilder result = new StringBuilder(path.replaceAll("/+$", ""));
        for (MenuRouteParam parameter : parameters) {
            result.append(PARAMETER_PREFIX).append(parameter.name());
        }
        return result.toString();
    }

    /**
     * 返回显式名称，未配置时按稳定菜单 ID 生成。
     * @param name 显式名称
     * @param id 菜单 ID
     * @return 路由名称
     */
    public static String name(String name, String id) {
        return name == null || name.isBlank() ? NAME_PREFIX + id : name;
    }
}
