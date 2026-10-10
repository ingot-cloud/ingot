package com.ingot.framework.authorization.field;

import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.*;
import lombok.RequiredArgsConstructor;

/**
 * <p>复用客户端对服务注册写操作的强制 fresh 契约，操作类别只信任服务器清单。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class ClientFieldPolicyProvider implements FieldPolicyProvider {
    private final AuthorizationAccess access;
    private final ResourceRegistry resources;

    @Override
    public AuthorizationDecision read(ResourceKey resource, String actionCode) {
        return access.preview(resource, actionCode);
    }

    @Override
    public AuthorizationDecision write(ResourceKey resource, String actionCode) {
        boolean mutating = resources.require(resource).descriptor().actions().stream()
                .anyMatch(action -> action.code().equals(actionCode) && action.mode() == ExecutionMode.MUTATING);
        if (!mutating)
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        return access.require(resource, actionCode);
    }
}
