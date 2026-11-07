package com.ingot.cloud.iam.domain.roleapplication;

import java.math.BigInteger;

/**
 * <p>确认事务提交后发出的本地领取提示，不携带请求上下文。</p>
 * @param taskId 已持久化任务
 * @param roleId 任务所属角色，仅用于广播关联
 * @author jy
 * @since 1.0.0
 */
public record RoleVersionApplicationReadyEvent(BigInteger taskId, BigInteger roleId) {
}
