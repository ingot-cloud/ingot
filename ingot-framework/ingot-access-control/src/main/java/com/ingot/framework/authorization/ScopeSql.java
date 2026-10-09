package com.ingot.framework.authorization;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;

/**
 * <p>把完整并集/交集合取范围落到可信实体列；列表、count与详情须共享同一调用。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ScopeSql {

    private static final String DENY = "1 = 0";

    private static final String ALLOW = "1 = 1";

    private ScopeSql() {
    }

    /**
     * 追加范围和租户隔离，列由代码注册，不能来自请求参数。
     * @param query 已绑定业务条件的查询
     * @param decision 同次决策
     * @param action 精确操作
     * @param objectId 对象ID列
     * @param ownerMemberId 归属成员列，资源不支持本人时可空
     * @param tenantId 真实租户列，平台可空
     * @param departments 可信部门关联谓词，不支持时可空
     * @param <T> 实体类型
     */
    public static <T> void restrict(LambdaQueryWrapper<T> query, AuthorizationDecision decision, String action,
            SFunction<T, ?> objectId, SFunction<T, ?> ownerMemberId, SFunction<T, ?> tenantId,
            DepartmentPredicate<T> departments) {
        if (decision == null || decision.expiresAt() == null || !java.time.Instant.now().isBefore(decision.expiresAt()))
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        var grant = decision.actions().get(action);
        if (grant == null || !grant.allowed())
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        if (decision.context().domain() == AuthorizationDomain.TENANT) {
            if (tenantId == null || decision.context().tenantId() == null)
                throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
            query.eq(tenantId, decision.context().tenantId());
        }
        if (grant.scope().isEmpty()) {
            query.apply(DENY);
            return;
        }
        query.and(union -> {
            boolean first = true;
            for (var clause : grant.scope()) {
                if (first) {
                    union.nested(part -> apply(part, clause, objectId, ownerMemberId, departments));
                    first = false;
                }
                else
                    union.or(part -> apply(part, clause, objectId, ownerMemberId, departments));
            }
        });
    }

    private static <T> void apply(LambdaQueryWrapper<T> query, ScopeCondition clause, SFunction<T, ?> id,
            SFunction<T, ?> owner, DepartmentPredicate<T> departments) {
        if (clause.all()) {
            query.apply(ALLOW);
            return;
        }
        if (clause.objectIds().isEmpty() && clause.ownerMemberId() == null && clause.departmentSets().isEmpty()) {
            query.apply(DENY);
            return;
        }
        if (!clause.objectIds().isEmpty())
            query.in(id, clause.objectIds());
        if (clause.ownerMemberId() != null) {
            if (owner == null)
                query.apply(DENY);
            else
                query.eq(owner, clause.ownerMemberId());
        }
        for (var values : clause.departmentSets()) {
            if (departments == null || values.isEmpty())
                query.apply(DENY);
            else
                query.and(part -> departments.restrict(part, values));
        }
    }

    /**
     * <p>业务代码登记部门关联谓词，不能使用请求提供的SQL。
     * </p>
     *
     * @param <T> 实体类型
     * @author jy
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface DepartmentPredicate<T> {

        /**
         * 追加一组需命中任一部门的条件。
         * @param query 当前合取
         * @param departmentIds 可信展开部门
         */
        void restrict(LambdaQueryWrapper<T> query, List<String> departmentIds);

    }

}
