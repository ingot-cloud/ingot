package com.ingot.framework.data.mybatis.scope.guard;

import java.util.List;
import java.util.Objects;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationResourceRuleDTO;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotHolder;
import com.ingot.framework.data.mybatis.scope.error.DataScopeErrorCode;
import com.ingot.framework.data.mybatis.scope.error.DataScopeException;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import org.springframework.security.authorization.AuthorizationDeniedException;

/**
 * <p>写入与归属变更的数据范围校验，与查询使用同一 {@code (resource, permission)} 规则。</p>
 *
 * <p>不判定功能准入。无匹配规则时 {@code ds_forbidden}；目标落在规则外或跨租户时 {@code AuthorizationDenied}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DataScopeGuard {

    private DataScopeGuard() {
    }

    /**
     * 断言目标记录落在当前用户对该资源操作的可写范围内。
     *
     * @param resource   资源编码
     * @param permission 规则键，与 {@code @DataScope} 使用同一操作码
     * @param target     目标租户、部门、归属用户
     * @throws AuthorizationDeniedException 跨租户或目标不在已匹配规则内
     * @throws DataScopeException           未认证、快照不可用或无匹配规则
     */
    public static void assertWritable(String resource, String permission, DataScopeTarget target) {
        if (StrUtil.isBlank(resource) || StrUtil.isBlank(permission)) {
            throw new DataScopeException(DataScopeErrorCode.DS_403);
        }
        InUser user = SecurityAuthContext.getUser();
        if (user == null || user.getId() == null) {
            throw new DataScopeException(DataScopeErrorCode.DS_401);
        }
        AuthorizationSnapshotDTO snapshot = AuthorizationSnapshotHolder.get();
        if (snapshot == null) {
            throw new DataScopeException(DataScopeErrorCode.DS_503);
        }
        if (target != null && target.getTenantId() != null
                && !Objects.equals(target.getTenantId(), user.getTenantId())) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        AuthorizationResourceRuleDTO rule = matchRule(snapshot, resource, permission);
        if (rule == null) {
            throw new DataScopeException(DataScopeErrorCode.DS_403);
        }
        if (rule.getScopeType() == DataScopeTypeEnum.ALL) {
            return;
        }
        boolean selfOk = BooleanUtil.isTrue(rule.getSelf())
                && target != null
                && Objects.equals(user.getId(), target.getUserId());
        boolean deptOk = target != null
                && target.getDeptId() != null
                && CollUtil.emptyIfNull(rule.getDeptIds()).contains(target.getDeptId());
        if (selfOk || deptOk) {
            return;
        }
        throw new AuthorizationDeniedException("AuthorizationDenied");
    }

    private static AuthorizationResourceRuleDTO matchRule(AuthorizationSnapshotDTO snapshot,
                                                          String resource,
                                                          String permission) {
        List<AuthorizationResourceRuleDTO> rules = snapshot.getResourceRules();
        if (CollUtil.isEmpty(rules)) {
            return null;
        }
        for (AuthorizationResourceRuleDTO rule : rules) {
            if (resource.equals(rule.getResourceCode()) && permission.equals(rule.getPermissionCode())) {
                return rule;
            }
        }
        return null;
    }
}
