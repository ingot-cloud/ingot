package com.ingot.framework.commons.model.iam;

import java.util.List;
import java.util.Map;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>安全中心的可信资源归属，操作编码保留历史契约。</p>
 * @author jy
 * @since 1.0.0
 */
public final class SecurityResources {
    /** 安全中心平台应用编码。 */
    public static final String APPLICATION = "platform:security";
    /** credential policy 资源编码。 */
    public static final String CREDENTIAL_POLICY = "credential-policy";
    /** lockout policy 资源编码。 */
    public static final String LOCKOUT_POLICY = "lockout-policy";
    /** login failure policy 资源编码。 */
    public static final String LOGIN_FAILURE_POLICY = "login-failure-policy";
    /** session 资源编码。 */
    public static final String SESSION = "session";
    /** session policy 资源编码。 */
    public static final String SESSION_POLICY = "session-policy";
    /** security policy 资源编码。 */
    public static final String SECURITY_POLICY = "security-policy";
    private SecurityResources() {
    }

    private static final List<ResourceDescriptor> DESCRIPTORS = List.of(
            descriptor(CREDENTIAL_POLICY, List.of(
                    IamAction.PLATFORM_CREDENTIAL_POLICY_READ, IamAction.PLATFORM_CREDENTIAL_POLICY_CREATE,
                    IamAction.PLATFORM_CREDENTIAL_POLICY_UPDATE, IamAction.PLATFORM_CREDENTIAL_POLICY_DELETE),
                    IamAction.VALUE_PLATFORM_CREDENTIAL_POLICY_READ),
            descriptor(LOCKOUT_POLICY, List.of(
                    IamAction.PLATFORM_LOCKOUT_POLICY_READ, IamAction.PLATFORM_LOCKOUT_POLICY_UPDATE),
                    IamAction.VALUE_PLATFORM_LOCKOUT_POLICY_READ),
            descriptor(LOGIN_FAILURE_POLICY, List.of(
                    IamAction.PLATFORM_LOGIN_FAILURE_POLICY_READ, IamAction.PLATFORM_LOGIN_FAILURE_POLICY_UPDATE),
                    IamAction.VALUE_PLATFORM_LOGIN_FAILURE_POLICY_READ),
            descriptor(SESSION, List.of(IamAction.PLATFORM_SESSION_READ, IamAction.PLATFORM_SESSION_REVOKE),
                    IamAction.VALUE_PLATFORM_SESSION_READ),
            descriptor(SESSION_POLICY, List.of(
                    IamAction.PLATFORM_SESSION_POLICY_READ, IamAction.PLATFORM_SESSION_POLICY_UPDATE),
                    IamAction.VALUE_PLATFORM_SESSION_POLICY_READ),
            descriptor(SECURITY_POLICY, List.of(
                    IamAction.PLATFORM_SECURITY_POLICY_READ, IamAction.PLATFORM_SECURITY_POLICY_CREATE,
                    IamAction.PLATFORM_SECURITY_POLICY_UPDATE, IamAction.PLATFORM_SECURITY_POLICY_DELETE),
                    IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ));

    /** 返回安全中心六种 ALL 资源的不可变执行描述。 */
    public static List<ResourceDescriptor> descriptors() {
        return DESCRIPTORS;
    }

    /** 精确查找已登记操作的完整资源键；非安全操作返回 null。 */
    public static ResourceKey key(IamAction action) {
        return DESCRIPTORS.stream()
                .filter(descriptor -> descriptor.actions().stream().anyMatch(a -> a.code().equals(action.getCode())))
                .map(ResourceDescriptor::key)
                .findFirst()
                .orElse(null);
    }

    private static ResourceDescriptor descriptor(String resource, List<IamAction> actions, String read) {
        return new ResourceDescriptor(new ResourceKey(AuthorizationDomain.PLATFORM, APPLICATION, resource),
                actions.stream().map(a -> new ActionDescriptor(a.getCode(), a.getOperation().isMutating()
                        ? ExecutionMode.MUTATING : ExecutionMode.READ_ONLY)).toList(),
                List.of(ScopeKind.ALL), List.of(), Map.of(), read, false);
    }
}
