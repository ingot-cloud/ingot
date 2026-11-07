package com.ingot.cloud.iam.domain.authorization;

import java.math.BigInteger;
import java.util.List;

/**
 * <p>一条有效分配对精确操作的字段贡献，范围已经施加来源委派上限。</p>
 * @param assignmentId 真实分配
 * @param groupId 组来源
 * @param delegationId 委派来源
 * @param revisionId 固定角色版本
 * @param actionCode 精确操作码
 * @param clauses 本贡献独立范围
 * @author jy
 * @since 1.0.0
 */
public record RoleFieldSource(BigInteger assignmentId, BigInteger groupId, Long delegationId,
        long revisionId, String actionCode, List<ScopeClause> clauses) {
    /** 复制贡献范围。 */
    public RoleFieldSource { clauses = List.copyOf(clauses); }
}
