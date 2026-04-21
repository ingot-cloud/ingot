package com.ingot.cloud.pms.identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.types.UserTenantType;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.account.domain.config.AccountDomainProperties;
import com.ingot.framework.account.domain.model.LockState;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.core.userdetails.InUserMetaKeys;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.tenant.TenantEnv;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : IdentityUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:52.</p>
 */
@Slf4j
public class IdentityUtil {
    /**
     * 填充认证上下文到 {@link UserDetailsResponse}（仅用户名/密码登录场景调用）
     * <p>
     * 语义分为两部分：
     * </p>
     * <ol>
     *   <li><b>硬决策</b>（阻断登录）：密码硬过期时将 {@code credentialsNonExpired = false}，
     *       Auth 侧会直接抛出 {@code CredentialsExpiredException}</li>
     *   <li><b>软上下文</b>（辅助决策）：通过 {@code meta} map 传递锁定时间、失败计数、阈值等，
     *       Auth 侧 {@code InUserDetailsChecker} / {@code DefaultUserCredentialChecker} 据此生成友好提示，
     *       key 参见 {@link InUserMetaKeys}</li>
     * </ol>
     * <p>宽限期 / 即将过期属于用户感知提示，不走此方法，由登录后的用户信息接口单独返回。</p>
     *
     * @param result                    待填充的响应对象
     * @param userId                    用户 ID
     * @param userType                  用户类型
     * @param credentialSecurityService 凭证安全服务（用于判定硬过期）
     * @param lockStatePort             锁定状态端口（用于读取 lockedUntil / failedLoginCount）
     * @param accountProperties         账号域配置（用于读取 maxAttempts / hintAfterAttempts）
     */
    public static void fillAuthContext(UserDetailsResponse result,
                                       Long userId,
                                       UserTypeEnum userType,
                                       CredentialSecurityService credentialSecurityService,
                                       LockStatePort lockStatePort,
                                       AccountDomainProperties accountProperties) {
        if (result == null) {
            return;
        }

        // 1. 硬过期判定：仅阻断登录时使用（禁用 / 锁定时跳过，前置状态足够阻断）
        if (Boolean.TRUE.equals(result.getEnabled()) && !Boolean.TRUE.equals(result.getLocked())) {
            try {
                PasswordCheckResult checkResult = credentialSecurityService.validate(
                        CredentialValidateRequest.builder()
                                .scene(CredentialScene.LOGIN)
                                .userId(userId)
                                .manualProcessError(true)
                                .build());
                if (!checkResult.isPassed()) {
                    result.setCredentialsNonExpired(false);
                }
            } catch (Exception e) {
                log.warn("[IdentityUtil] 凭证过期检查异常，放行登录 userId={}", userId, e);
            }
        }

        // 2. 软上下文：锁定时间 / 失败计数 / 阈值 / 提示节奏
        Map<String, Object> meta = buildAuthMeta(userId, userType, result, lockStatePort, accountProperties);
        if (!meta.isEmpty()) {
            result.setMeta(meta);
        }
    }

    private static Map<String, Object> buildAuthMeta(Long userId,
                                                     UserTypeEnum userType,
                                                     UserDetailsResponse result,
                                                     LockStatePort lockStatePort,
                                                     AccountDomainProperties accountProperties) {
        Map<String, Object> meta = new HashMap<>(4);

        AccountDomainProperties.LockoutPolicy policy =
                accountProperties != null ? accountProperties.getLockout() : null;
        if (policy != null && policy.isEnabled()) {
            meta.put(InUserMetaKeys.MAX_FAILED_ATTEMPTS, policy.getMaxAttempts());
            meta.put(InUserMetaKeys.HINT_AFTER_ATTEMPTS, policy.getHintAfterAttempts());
        }

        try {
            Optional<LockState> lockStateOpt = lockStatePort.findByUser(userId, userType);
            if (lockStateOpt.isPresent()) {
                LockState lockState = lockStateOpt.get();
                // 锁定状态下，若为临时锁定则传递到期时间，供 InUserDetailsChecker 计算剩余分钟数
                // 注意：meta 经 Feign/Jackson 传递到 Auth，为避免因两端 JavaTimeModule 配置差异
                // 导致 LocalDateTime 被序列化为数组/时间戳，这里统一使用 ISO-8601 字符串传输，
                // 接收端 InUser.getMetaValue(LOCKED_UNTIL, LocalDateTime.class) 会透明解析。
                if (Boolean.TRUE.equals(result.getLocked()) && lockState.getLockedUntil() != null) {
                    meta.put(InUserMetaKeys.LOCKED_UNTIL, lockState.getLockedUntil().toString());
                }
                // 未锁定时传递当前失败次数，供 DefaultUserCredentialChecker 做分级提示
                if (!Boolean.TRUE.equals(result.getLocked())
                        && lockState.getFailedLoginCount() != null) {
                    meta.put(InUserMetaKeys.FAILED_LOGIN_COUNT, lockState.getFailedLoginCount());
                }
            }
        } catch (Exception e) {
            log.warn("[IdentityUtil] 查询锁定状态异常，跳过 meta 填充 userId={}", userId, e);
        }

        return meta;
    }

    /**
     * 映射用户信息
     *
     * @param user                 用户信息
     * @param userType             用户类型
     * @param tenant               租户ID
     * @param sysTenantService     租户服务
     * @param sysUserTenantService 用户租户服务
     * @param bizUserService       用户服务
     * @param bizAppService        应用服务
     * @param bizRoleService       角色服务
     * @return 用户信息
     */
    public static UserDetailsResponse map(SysUser user,
                                          UserTypeEnum userType,
                                          Long tenant,
                                          SysTenantService sysTenantService,
                                          SysUserTenantService sysUserTenantService,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<TenantMainDTO> allows = getAllowTenants(user, sysTenantService, sysUserTenantService);

                    // 租户维度可访问性：allows 不为空，且登录 tenant 在允许列表内
                    boolean tenantAccessible = CollUtil.isNotEmpty(allows)
                            && (tenant == null || allows.stream()
                            .anyMatch(item -> Long.parseLong(item.getId()) == tenant));

                    // 账号维度：来自 sys_user.enabled / sys_user.locked
                    boolean userEnabled = Boolean.TRUE.equals(value.getEnabled()) && tenantAccessible;
                    boolean userLocked = Boolean.TRUE.equals(value.getLocked());

                    UserDetailsResponse result = UserConvert.INSTANCE.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);
                    result.setEnabled(userEnabled);
                    result.setLocked(userLocked);

                    // 如果账号不可用（禁用或锁定）则不需要查询 scope，直接返回
                    if (!userEnabled || userLocked) {
                        return result;
                    }

                    // 设置用户 Scope
                    List<String> scopes = new ArrayList<>();
                    // 强制修改密码
                    if (BooleanUtil.isTrue(user.getMustChangePwd())) {
                        scopes.add(PermissionConstants.INIT_PASSWORD);
                        result.setScopes(scopes);
                        return result;
                    }

                    // 确认登录的租户不为空，那么查询用户在当前租户下的Scope
                    if (tenant != null) {
                        scopes.addAll(getScopes(tenant, user, bizUserService, bizAppService, bizRoleService));
                    } else {
                        scopes.addAll(allows.stream()
                                .flatMap(org ->
                                        TenantEnv.applyAs(Long.parseLong(org.getId()),
                                                        () -> getScopes(Long.parseLong(org.getId()), user,
                                                                bizUserService, bizAppService, bizRoleService))
                                                .stream())
                                .toList());
                    }
                    result.setScopes(scopes);
                    return result;
                }).orElse(null));
    }

    private static List<TenantMainDTO> getAllowTenants(SysUser user,
                                                       SysTenantService sysTenantService,
                                                       SysUserTenantService sysUserTenantService) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.getUserOrgs(user.getId());
        if (CollUtil.isEmpty(userTenantList)) {
            return ListUtil.empty();
        }
        return BizUtils.getTenants(sysTenantService,
                userTenantList.stream()
                        .map(UserTenantType::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t ->
                                Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())));
    }

    private static List<String> getScopes(Long tenant,
                                          SysUser user,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService) {
        // 查询所有角色
        List<RoleType> roles = bizUserService.getUserRoles(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        // InAuthorityUtils.authorityWithTenant 包装角色编码
        List<String> scopes = new ArrayList<>(getRoleCodes(roles, tenant));
        // 查询组织不可用应用
        List<MetaApp> disabledApps = bizAppService.getDisabledApps();
        List<String> authorities = bizRoleService.getRolesPermissions(roles).stream()
                .filter(auth -> disabledApps.stream()
                        .noneMatch(app -> Objects.equals(auth.getId(), app.getPermissionId())))
                .map(auth -> InAuthorityUtils.authorityWithTenant(auth.getCode(), tenant))
                .toList();
        scopes.addAll(authorities);

        return scopes;
    }

    private static List<String> getRoleCodes(List<? extends RoleType> roles, Long loginTenant) {
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return roles.stream()
                .map(item ->
                        InAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant))
                .toList();
    }
}
