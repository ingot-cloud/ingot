package com.ingot.cloud.member.common;

import java.util.function.Predicate;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.domain.MemberRole;

/**
 * <p>Description  : BizFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/26.</p>
 * <p>Time         : 6:59 PM.</p>
 */
public final class BizFilter {

    public static Predicate<MemberRole> roleFilter(MemberRole condition) {
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

    public static Predicate<MemberPermission> authorityFilter(MemberPermission condition) {
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
}
