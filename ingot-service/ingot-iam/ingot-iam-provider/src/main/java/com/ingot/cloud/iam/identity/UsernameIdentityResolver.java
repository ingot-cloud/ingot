package com.ingot.cloud.iam.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.SysUser;
import com.ingot.cloud.iam.authorization.engine.EffectiveAuthorizationService;
import com.ingot.cloud.iam.service.biz.BizUserDeptService;
import com.ingot.cloud.iam.service.domain.SysTenantService;
import com.ingot.cloud.iam.service.domain.SysUserService;
import com.ingot.cloud.iam.service.domain.SysUserTenantService;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.security.account.web.support.AuthContextSupport;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.core.identity.UserIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>优先按新模型账号选择单一成员身份，未命中时回退旧用户表。</p>
 *
 * <p>新模型命中后不再合并旧授权快照；旧路径不写入 AuthorizationContext，不能调用新管理接口。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UsernameIdentityResolver implements UserIdentityResolver {
    private final AccountIdentityService accountIdentities;
    private final SysUserService sysUserService;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;

    private final EffectiveAuthorizationService effectiveAuthorizationService;
    private final BizUserDeptService bizUserDeptService;
    private final AuthContextSupport authContextSupport;

    private final OssService ossService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return type == UserIdentityTypeEnum.USERNAME;
    }

    /**
     * 加载登录资料；新模型账号存在时必须走成员选择，不得回退 SysUser。
     *
     * @param request 登录条件
     * @return 认证资料；新旧路径均未命中时为空
     */
    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        var modern = accountIdentities.load(request);
        if (modern.isPresent()) {
            UserDetailsResponse response = modern.get();
            if (response.getId() != null) {
                authContextSupport.fill(response, response.getId(), request.getUserType());
            }
            return response;
        }
        String username = request.getUsername();
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, username));
        if (user == null) {
            user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, username));
        }
        UserDetailsResponse response = IdentityUtil.map(user, request.getUserType(), request.getTenant(),
                sysTenantService, sysUserTenantService,
                effectiveAuthorizationService, bizUserDeptService, ossService);
        if (user != null) {
            authContextSupport.fill(response, user.getId(), request.getUserType());
        }
        return response;
    }
}
