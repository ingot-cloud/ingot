package com.ingot.cloud.iam.organization;

import java.util.Set;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>在成员写事务内检查精确 ACTION，并在求值器可用时校验对象范围覆盖。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class GrantPresenceMemberGuard implements MemberMutationGuard {
    private final IamActionAuthorizer authorizer;
    private final ResourceAccess access;

    /**
     * 仅校验 ACTION 存在性，供无完整求值器的单元测试使用。
     *
     * @param authorizer 事务内可见的授权查询
     */
    public GrantPresenceMemberGuard(IamActionAuthorizer authorizer) {
        this(authorizer, null);
    }

    /**
     * 绑定精确 ACTION 与对象范围校验。
     *
     * @param authorizer 事务内可见的授权查询
     * @param access 对象范围；测试可空
     */
    @Autowired
    public GrantPresenceMemberGuard(IamActionAuthorizer authorizer, ResourceAccess access) {
        this.authorizer = authorizer;
        this.access = access;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void require(AuthorizationContext actor, Operation operation, String memberId,
                        Set<String> oldDepartments, Set<String> newDepartments) {
        if (actor == null || operation == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        IamAction action = action(actor.domain(), operation);
        authorizer.require(actor, action);
        if (access != null) {
            access.requireMemberWrite(actor, action, memberId, operation, oldDepartments, newDepartments);
        }
    }

    private static IamAction action(AuthorizationDomain domain, Operation operation) {
        return switch (operation) {
            case CHANGE_STATUS -> domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_MEMBER_STATUS : IamAction.TENANT_MEMBER_STATUS;
            case REMOVE -> domain == AuthorizationDomain.PLATFORM
                    ? IamAction.PLATFORM_MEMBER_REMOVE : IamAction.TENANT_MEMBER_REMOVE;
            case CHANGE_DEPARTMENTS -> {
                if (domain != AuthorizationDomain.TENANT) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
                yield IamAction.TENANT_MEMBER_DEPARTMENTS;
            }
        };
    }
}
