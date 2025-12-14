package com.ingot.cloud.pms.common;

import java.util.function.Predicate;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.types.MenuType;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;

/**
 * <p>Description  : BizFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/26.</p>
 * <p>Time         : 6:59 PM.</p>
 */
public final class BizFilter {

    /**
     * 菜单过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<MenuTreeNodeVO> menuFilter(MenuType condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            if (condition.getOrgType() != null) {
                return item.getOrgType() == condition.getOrgType();
            }
            return true;
        };
    }

    /**
     * 权限过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<PermissionType> authorityFilter(PermissionType condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            if (condition.getOrgType() != null) {
                return item.getOrgType() == condition.getOrgType();
            }
            return true;
        };
    }

    /**
     * 部门过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<TenantDept> deptFilter(TenantDept condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            return true;
        };
    }

    public static Predicate<RoleType> roleFilter(RoleType condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            if (condition.getOrgType() != null) {
                return item.getOrgType() == condition.getOrgType();
            }
            if (condition.getFilterDept() != null) {
                return item.getFilterDept() == condition.getFilterDept();
            }
            return true;
        };
    }
}
