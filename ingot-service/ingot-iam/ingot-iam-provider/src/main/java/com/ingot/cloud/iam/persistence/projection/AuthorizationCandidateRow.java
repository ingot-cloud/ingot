package com.ingot.cloud.iam.persistence.projection;

/**
 * <p>授权候选数据库投影，动态 SQL 只使用服务端固定查询定义。</p>
 * @param id 对象真实标识
 * @param name 对象显示名称
 * @param kind 角色版本种类
 * @param revision 角色版本号
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationCandidateRow(java.math.BigInteger id, String name,
        com.ingot.framework.commons.model.iam.RoleKind kind, java.math.BigInteger revision) {
}
