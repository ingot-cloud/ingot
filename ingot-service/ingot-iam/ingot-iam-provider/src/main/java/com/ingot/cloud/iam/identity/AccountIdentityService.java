package com.ingot.cloud.iam.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>在凭证查询命中新模型账号后选择单一域成员，不把旧授权快照并入新身份。</p>
 *
 * <p>账号不存在时返回空，供登录加载器回退旧表；账号已命中后身份无效直接失败，不再查询 SysUser。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AccountIdentityService {
    private final AccountCredentialRepository accounts;
    private final ActiveIdentityService identities;

    /**
     * 尝试按新模型加载认证资料。
     *
     * @param request Auth 传入的登录条件；tenant 为空表示选择平台身份
     * @return 新模型资料；账号表不存在或账号未命中时为空
     * @throws com.ingot.framework.commons.error.BizException 账号存在但目标域成员无效
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
        UserDetailsResponse result = new UserDetailsResponse();
        result.setId(account.id());
        result.setUsername(account.username());
        result.setPassword(account.passwordHash());
        result.setEnabled(account.enabled());
        result.setLocked(account.locked());
        result.setUserType(request.getUserType().getValue());
        if (!account.enabled() || account.locked()) {
            return result;
        }
        AuthorizationDomain domain = request.getTenant() == null
                ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT;
        String tenantId = request.getTenant() == null ? null : Long.toString(request.getTenant());
        ActiveIdentity identity = identities.selectAuthenticated(Long.toString(account.id()), domain, tenantId);
        result.setAuthorizationContext(identity.context());
        result.setTenant(request.getTenant());
        result.setAllows(accounts.tenantAllows(Long.toString(account.id())));
        if (domain == AuthorizationDomain.TENANT) {
            result.setDeptIds(accounts.departments(tenantId, identity.context().memberId()));
        } else {
            result.setDeptIds(List.of());
        }
        List<String> scopes = new ArrayList<>();
        if (account.mustChangePassword()) {
            scopes.add(PermissionConstants.INIT_PASSWORD);
        }
        result.setScopes(scopes);
        return result;
    }
}
