package com.ingot.cloud.iam.authorization;

import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamAction;

/**
 * <p>在写操作前校验当前身份是否拥有精确 ACTION，不根据角色名或平台成员资格放行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface IamActionAuthorizer {
    /**
     * 确认 actor 对指定 ACTION 有授权；拒绝时必须抛出异常。
     *
     * @param actor 已通过身份恢复的当前成员
     * @param action 本次接口声明的 ACTION
     * @throws com.ingot.framework.commons.error.BizException 无授权为 ActionDenied；基础设施故障为 AuthorizationUnavailable
     */
    void require(AuthorizationContext actor, IamAction action);
}
