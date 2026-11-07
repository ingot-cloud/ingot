package com.ingot.cloud.iam.infrastructure.persistence.projection;

import java.math.BigInteger;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationItemStatus;

/**
 * <p>同一分配可见范围内的任务明细状态聚合行。</p>
 * @author jy
 * @since 1.0.0
 */
public record RoleVersionApplicationCount(BigInteger taskId, RoleVersionApplicationItemStatus status, long total) {
}
