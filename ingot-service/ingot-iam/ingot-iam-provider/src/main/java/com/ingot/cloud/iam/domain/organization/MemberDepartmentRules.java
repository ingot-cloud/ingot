package com.ingot.cloud.iam.domain.organization;

import java.util.HashSet;
import java.util.List;

/**
 * <p>校验成员任职集合的重复部门和主部门数量，归属与空值交由各入口校验。</p>
 * @author jy
 * @since 1.0.0
 */
public final class MemberDepartmentRules {
    private MemberDepartmentRules() { }

    /** 判断部门是否唯一且至多一个主部门；空值由Bean Validation处理。 */
    public static boolean hasValidShape(List<MemberDepartmentBinding> departments) {
        if (departments == null || departments.contains(null)) {
            return true;
        }
        var ids = new HashSet<String>();
        int primary = 0;
        for (MemberDepartmentBinding department : departments) {
            if (!ids.add(department.id()) || (department.primary() && ++primary > 1)) {
                return false;
            }
        }
        return true;
    }

}
