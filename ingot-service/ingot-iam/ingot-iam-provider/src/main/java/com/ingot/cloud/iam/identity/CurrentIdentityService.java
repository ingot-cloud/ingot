package com.ingot.cloud.iam.identity;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>从已认证安全上下文恢复 IAM 成员身份，并查询数据库确认当前状态。</p>
 *
 * <p>不读取租户请求头、query 或角色名，不把 Member 用户体系或缺失成员信息的旧会话视为 IAM 身份。
 * 本服务只校验身份，业务操作仍必须调用授权引擎。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CurrentIdentityService {
    private final ActiveIdentityService identities;

    /**
     * 返回已认证会话中的当前有效成员，不接受请求中的替代身份。
     * @return 经过实时状态验证的成员及其版本
     * @throws BizException 缺失可信成员上下文时使用 IdentityInvalid；数据库故障使用 AuthorizationUnavailable
     */
    public ActiveIdentity requireCurrent() {
        var authentication = SecurityAuthContext.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof InUser user)
                || UserTypeEnum.getEnum(user.getUserType()) != UserTypeEnum.ADMIN
                || user.getAuthorizationContext() == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return identities.requireActive(user.getAuthorizationContext());
    }

    /**
     * 限定当前接口的管理域；不能借平台租户管理路径进入租户成员域。
     * @param domain 接口在服务器声明的管理域，不能取自客户端参数
     * @return 域匹配且状态有效的成员
     * @throws BizException 域不匹配时使用 ActionDenied，身份无效或基础设施故障按实时校验失败
     */
    public ActiveIdentity requireDomain(AuthorizationDomain domain) {
        if (domain == null) {
            throw new IllegalArgumentException("接口管理域必填");
        }
        ActiveIdentity current = requireCurrent();
        if (current.context().domain() != domain) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return current;
    }
}
