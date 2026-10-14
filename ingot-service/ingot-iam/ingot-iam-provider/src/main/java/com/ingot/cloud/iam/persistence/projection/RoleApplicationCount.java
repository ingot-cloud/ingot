package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;
import com.ingot.framework.commons.model.iam.RoleApplicationItemStatus;

/**
 * <p>同一分配可见范围内的任务明细状态聚合行。</p>
 * @author jy
 * @since 1.0.0
 */
public record RoleApplicationCount(BigInteger taskId, RoleApplicationItemStatus status, long total) {
}
