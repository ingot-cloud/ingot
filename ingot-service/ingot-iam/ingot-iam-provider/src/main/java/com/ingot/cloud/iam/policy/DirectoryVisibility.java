package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;

/**
 * <p>通讯录可见成员谓词：默认或允许并集，扣除禁止后始终包含查看者本人。</p>
 *
 * @param coversAll 是否覆盖当前租户全部未移出成员（再扣除 {@code excluded}）
 * @param included 显式允许的成员；全域时为空
 * @param excluded 全域时扣除的禁止目标；显式集合时为空
 * @param viewerId 查看者，禁止规则也不能隐藏其本人基础资料
 * @author jy
 * @since 1.0.0
 */
public record DirectoryVisibility(boolean coversAll, Set<Long> included, Set<Long> excluded, long viewerId) {
    /**
     * 复制集合，避免求值结果被调用方改写。
     */
    public DirectoryVisibility {
        included = copy(included);
        excluded = copy(excluded);
    }

    /**
     * 构造扣除禁止目标后的全组织可见范围，本人即使被禁止仍可见。
     *
     * @param excluded 禁止目标
     * @param viewerId 查看者
     * @return 全域谓词
     */
    public static DirectoryVisibility all(Set<Long> excluded, long viewerId) {
        return new DirectoryVisibility(true, Set.of(), excluded, viewerId);
    }

    /**
     * 构造显式成员集合，调用方须已扣除禁止目标；本人始终并入。
     *
     * @param included 可见成员
     * @param viewerId 查看者
     * @return 显式谓词
     */
    public static DirectoryVisibility explicit(Set<Long> included, long viewerId) {
        Set<Long> ids = new LinkedHashSet<>(included == null ? Set.of() : included);
        ids.add(viewerId);
        return new DirectoryVisibility(false, ids, Set.of(), viewerId);
    }

    /**
     * 判断目标是否对查看者可见。
     *
     * @param memberId 目标成员
     * @return 可见时为 true
     */
    public boolean contains(long memberId) {
        if (memberId == viewerId) {
            return true;
        }
        if (coversAll) {
            return !excluded.contains(memberId);
        }
        return included.contains(memberId);
    }

    /**
     * 判断本范围是否比预览范围更窄，因而存在不能向操作者披露的预览结果。
     *
     * @param preview 预览查看者的可见范围
     * @return 操作者看不见预览中某些成员时为 true
     */
    public boolean concealsAny(DirectoryVisibility preview) {
        if (preview == null) {
            return false;
        }
        if (preview.coversAll && !coversAll) {
            return true;
        }
        if (preview.coversAll) {
            for (Long memberId : excluded) {
                if (memberId != viewerId && preview.contains(memberId)) {
                    return true;
                }
            }
            return !contains(preview.viewerId);
        }
        for (Long memberId : preview.included) {
            if (!contains(memberId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把可见范围编译成对象范围，供原值筛选按可能匹配目标做披露校验。
     *
     * @return 全域、空集或显式 ID 条款
     */
    public ObjectScope toLookupScope() {
        if (coversAll) {
            return ObjectScope.all();
        }
        if (included.isEmpty()) {
            return ObjectScope.none();
        }
        Set<BigInteger> ids = new LinkedHashSet<>();
        for (Long memberId : included) {
            ids.add(BigInteger.valueOf(memberId));
        }
        return ObjectScope.of(List.of(new ObjectScopeClause(ids, List.of())));
    }

    private static Set<Long> copy(Set<Long> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
