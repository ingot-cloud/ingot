package com.ingot.cloud.iam.persistence.projection;

/**
 * <p>授权候选数据库投影，动态 SQL 只使用服务端固定查询定义。</p>
 * @param id 对象真实标识
 * @param name 对象显示名称
 * @param kind 角色版本种类
 * @param revision 角色版本号
 * @param resourceName 操作候选所属资源名称；其他候选为空
 * @param parentId 层级候选父节点
 * @param hasChildren 层级候选是否有子节点
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationCandidateRow(java.math.BigInteger id, String name,
        com.ingot.framework.commons.model.iam.RoleKind kind, java.math.BigInteger revision,
        String resourceName, java.math.BigInteger parentId, Boolean hasChildren) {
    /**
     * 保持普通列表候选测试与调用方的构造方式。
     */
    public AuthorizationCandidateRow(java.math.BigInteger id, String name,
            com.ingot.framework.commons.model.iam.RoleKind kind, java.math.BigInteger revision,
            String resourceName) {
        this(id, name, kind, revision, resourceName, null, null);
    }
}
