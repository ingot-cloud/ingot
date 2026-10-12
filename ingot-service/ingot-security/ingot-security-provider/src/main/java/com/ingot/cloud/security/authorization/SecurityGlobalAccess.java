package com.ingot.cloud.security.authorization;

import java.util.List;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.SecurityResources;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>执行安全中心 ALL 范围，写操作由 SDK 强制 fresh 求值。</p>
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SecurityGlobalAccess {
    private final AuthorizationAccess authorization;

    /**
     * 校验服务器声明的精确操作及全局目标；依赖故障拒绝执行。
     * @param action 已登记的安全中心操作
     * @throws SdkAuthorizationException 操作、身份、范围或有效期校验失败
     */
    public void require(IamAction action) {
        var key = SecurityResources.key(action);
        if (key == null) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
        var decision = authorization.require(key, action.getCode());
        if (!key.equals(decision.resource()) || decision.context().domain() != AuthorizationDomain.PLATFORM) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
        authorization.requireTarget(decision, action.getCode(), new ScopeTarget(null, null, null, List.of()));
    }
}
