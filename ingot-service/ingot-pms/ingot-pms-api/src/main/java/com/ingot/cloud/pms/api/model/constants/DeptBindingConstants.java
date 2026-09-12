package com.ingot.cloud.pms.api.model.constants;

/**
 * <p>部门角色绑定的规范化常量，用于唯一约束哨兵值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DeptBindingConstants {

    /**
     * 空部门在唯一索引生成列中的哨兵值；真实部门主键不得使用该值。
     * 业务参数继续用 {@code null} 表示非部门绑定。
     */
    public static final long NULL_DEPT_SENTINEL = 0L;

    private DeptBindingConstants() {
    }

    /**
     * 将业务部门 ID 规范化为唯一键分量。
     *
     * @param deptId 部门 ID，{@code null} 表示租户级任职
     * @return 非空部门 ID 或 {@link #NULL_DEPT_SENTINEL}
     */
    public static long normalizeDeptId(Long deptId) {
        return deptId == null ? NULL_DEPT_SENTINEL : deptId;
    }

    /**
     * 判断规范化值是否表示非部门绑定。
     *
     * @param normalizedDeptId {@link #normalizeDeptId(Long)} 的结果
     * @return 为哨兵值时返回 {@code true}
     */
    public static boolean isTenantLevel(long normalizedDeptId) {
        return normalizedDeptId == NULL_DEPT_SENTINEL;
    }
}
