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
     * 确认 actor 对指定 ACTION 有授权，并给出该授权是否来自完整治理资格。
     *
     * @param actor 已通过身份恢复的当前成员
     * @param action 本次接口声明的 ACTION
     * @return 准入结论
     * @throws com.ingot.framework.commons.error.BizException 无授权为 ActionDenied；基础设施故障为 AuthorizationUnavailable
     */
    Admission admit(AuthorizationContext actor, IamAction action);

    /**
     * 确认 actor 对指定 ACTION 有授权；拒绝时必须抛出异常。
     *
     * @param actor 已通过身份恢复的当前成员
     * @param action 本次接口声明的 ACTION
     * @throws com.ingot.framework.commons.error.BizException 无授权为 ActionDenied；基础设施故障为 AuthorizationUnavailable
     */
    default void require(AuthorizationContext actor, IamAction action) {
        admit(actor, action);
    }

    /**
     * <p>一次 ACTION 准入的结论，区分完整治理资格与只能由委派派生的受限资格。</p>
     *
     * <p>受限方创建授权时必须绑定属于自己的单一有效委派，不能省略来源、借用他人委派或拼接多条委派的不同维度。</p>
     *
     * @param governed 该 ACTION 至少来自一条非委派授权时为 true
     * @author jy
     * @since 1.0.0
     */
    record Admission(boolean governed) {
    }
}
