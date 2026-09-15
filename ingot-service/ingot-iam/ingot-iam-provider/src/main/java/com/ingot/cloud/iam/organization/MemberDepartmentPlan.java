package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.HashSet;

/**
 * <p>保存服务器校验后的完整任职目标，允许无部门且非空时最多一个主部门。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 成员读取版本
 * @param departments 完整目标关系；删除、增加和主部门变化均参与事务内范围校验
 */
public record MemberDepartmentPlan(String expectedVersion, List<Department> departments) {
    /**
     * 冻结目标关系并拒绝重复部门或多个主部门。
     * @throws IllegalArgumentException 版本为空、部门重复或主部门超过一个
     */
    public MemberDepartmentPlan {
        if (expectedVersion == null || expectedVersion.isBlank() || departments == null) {
            throw new IllegalArgumentException("版本和目标关系必填");
        }
        departments = List.copyOf(departments);
        var ids = new HashSet<String>();
        int primaryCount = 0;
        for (Department department : departments) {
            if (!ids.add(department.id()) || (department.primary() && ++primaryCount > 1)) {
                throw new IllegalArgumentException("部门不能重复且最多一个主部门");
            }
        }
    }

    /**
     * <p>描述一个目标部门及是否作为主部门，不携带可修改的部门资料。</p>
     * @author jy
     * @since 1.0.0
     * @param id 当前租户部门 ID
     * @param primary 是否主部门
     */
    public record Department(String id, boolean primary) {
        /** 校验部门 ID 必填，数据库格式在持久化边界验证。 */
        public Department {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("部门 ID 必填");
            }
        }
    }
}
