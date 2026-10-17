package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;

/**
 * <p>当前页部门的直属 ACTIVE 成员计数。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param departmentId 部门 ID
 * @param memberCount 直属 ACTIVE 成员数
 */
public record DepartmentMemberCount(BigInteger departmentId, Integer memberCount) {
}
