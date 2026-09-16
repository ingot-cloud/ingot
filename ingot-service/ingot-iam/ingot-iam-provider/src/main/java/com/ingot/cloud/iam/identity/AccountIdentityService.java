package com.ingot.cloud.iam.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>在凭证查询命中新模型账号后选择单一域成员，不把旧授权快照并入新身份。</p>
 *
 * <p>账号不存在时返回空，由认证入口按失败关闭处理；账号已命中后身份无效直接失败，不再查询 SysUser。
 * 管理域取认证入口声明的 domain：平台分支 tenantId 为空且不返回允许访问的租户集合；租户分支未带
 * tenant 时只返回成员资格候选，不建立授权上下文；domain 缺省时保留既有兼容推断。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AccountIdentityService {
    private final AccountCredentialRepository accounts;
    private final ActiveIdentityService identities;
    private final AuthorizationEvaluator evaluator;

    /**
     * 尝试按新模型加载认证资料。
     *
     * @param request Auth 传入的登录条件；domain 声明目标管理域，缺省时按 tenant 兼容推断
     * @return 新模型资料；账号表不存在或账号未命中时为空
     * @throws BizException 账号存在但目标域成员无效，或平台域携带了 tenant
     */
    public Optional<UserDetailsResponse> load(UserDetailsRequest request) {
        Optional<AccountCredentialRepository.AccountCredentials> found;
        try {
            found = accounts.findByLogin(request.getUsername());
        } catch (DataAccessException exception) {
            return Optional.empty();
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(map(found.get(), request));
    }

    /**
     * 按账号 ID 加载新模型资料，供社交绑定命中后选择单一域成员。
     *
     * @param accountId 全局账号 ID
     * @param request 登录条件
     * @return 新模型资料；账号不存在或目标表不可用时为空
     */
    public Optional<UserDetailsResponse> loadByAccountId(long accountId, UserDetailsRequest request) {
        Optional<AccountCredentialRepository.AccountCredentials> found;
        try {
            found = accounts.findById(accountId);
        } catch (DataAccessException exception) {
            return Optional.empty();
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(map(found.get(), request));
    }

    private UserDetailsResponse map(AccountCredentialRepository.AccountCredentials account,
                                    UserDetailsRequest request) {
        boolean locked = accounts.locked(account.id());
        UserDetailsResponse result = new UserDetailsResponse();
        result.setId(account.id());
        result.setUsername(account.username());
        result.setPassword(account.passwordHash());
        result.setEnabled(account.enabled());
        result.setLocked(locked);
        result.setUserType(request.getUserType().getValue());
        if (!account.enabled() || locked) {
            return result;
        }

        String accountId = Long.toString(account.id());
        AuthorizationDomain domain = resolveDomain(request);
        if (domain == AuthorizationDomain.TENANT && request.getTenant() == null) {
            // 成员资格选择阶段：只给出候选组织，不建立成员上下文也不授予任何操作。
            result.setAllows(accounts.tenantAllows(accountId));
            result.setDeptIds(List.of());
            result.setScopes(initialScopes(account, null));
            return result;
        }

        String tenantId = domain == AuthorizationDomain.PLATFORM ? null : Long.toString(request.getTenant());
        ActiveIdentity identity = identities.selectAuthenticated(accountId, domain, tenantId);
        result.setAuthorizationContext(identity.context());
        result.setTenant(domain == AuthorizationDomain.PLATFORM ? null : request.getTenant());
        if (domain == AuthorizationDomain.TENANT) {
            // 租户分支沿用既有认证协议的成员资格选择集合。
            result.setAllows(accounts.tenantAllows(accountId));
            result.setDeptIds(accounts.departments(tenantId, identity.context().memberId()));
        } else {
            // 平台身份不返回允许访问的租户，避免把租户成员资格混入平台响应。
            result.setAllows(List.of());
            result.setDeptIds(List.of());
        }
        result.setScopes(initialScopes(account, identity.context()));
        return result;
    }

    /**
     * 按认证入口声明的管理域解析目标域；缺省时保留既有兼容推断。
     * 平台域携带 tenant 属于非法请求，不静默降级为租户登录。
     */
    private AuthorizationDomain resolveDomain(UserDetailsRequest request) {
        AuthorizationDomain declared = request.getDomain();
        if (declared == null) {
            return request.getTenant() == null ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT;
        }
        if (declared == AuthorizationDomain.PLATFORM && request.getTenant() != null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return declared;
    }

    private List<String> initialScopes(AccountCredentialRepository.AccountCredentials account,
                                       AuthorizationContext context) {
        List<String> scopes = new ArrayList<>();
        if (account.mustChangePassword()) {
            scopes.add(PermissionConstants.INIT_PASSWORD);
        }
        if (context != null) {
            scopes.addAll(evaluator.evaluate(context).actionCodes());
        }
        return scopes;
    }
}
