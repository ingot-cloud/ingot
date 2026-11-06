package com.ingot.cloud.member.authorization;

import java.util.List;

import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>校验会员后台的精确平台操作和 ALL 范围。</p>
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberGlobalAccess {
    private static final ScopeTarget GLOBAL_TARGET = new ScopeTarget(null, null, null, List.of());

    private final AuthorizationAccess authorization;

    /** 使用 SDK 执行可信操作，写权限按请求实时求值。 */
    public void require(ResourceKey key, String action) {
        var decision = authorization.require(key, action);
        if (!key.equals(decision.resource()) || decision.context().domain() != AuthorizationDomain.PLATFORM) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
        authorization.requireTarget(decision, action, GLOBAL_TARGET);
    }
}
