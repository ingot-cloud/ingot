package com.ingot.cloud.pms.common;

import java.util.function.Predicate;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysDept;

/**
 * <p>Description  : BizFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/26.</p>
 * <p>Time         : 6:59 PM.</p>
 */
public final class BizFilter {

    /**
     * 权限过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<SysAuthority> authorityFilter(SysAuthority condition) {
        return (item) -> {
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
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
    public static Predicate<SysDept> deptFilter(SysDept condition) {
        return (item) -> {
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            return true;
        };
    }
}
