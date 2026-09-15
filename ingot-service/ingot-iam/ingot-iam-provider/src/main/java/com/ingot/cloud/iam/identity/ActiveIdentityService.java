package com.ingot.cloud.iam.identity;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>从数据库确认当前成员身份仍有效，身份无效和授权基础设施不可用分别失败关闭。</p>
 *
 * <p>调用方必须先认证凭证；本服务不接受身份切换请求、不授予业务操作，也不使用旧身份回退。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ActiveIdentityService {
    private final IdentityRepository identities;

    /**
     * 验证已认证会话所引用的当前身份及关联全局账号仍然有效。
     *
     * @param context 可信会话候选身份
     * @return 同次查询得到的有效身份和状态版本；不代表已获任何业务权限
     * @throws BizException 无效身份返回 IdentityInvalid，数据库失败返回 AuthorizationUnavailable
     */
    public ActiveIdentity requireActive(AuthorizationContext context) {
        try {
            return identities.findActive(context).orElseThrow(() -> new BizException(IamReasonCode.IDENTITY_INVALID));
        } catch (DataAccessException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
    }
    /**
     * 为已通过凭证验证的账号建立一个明确的成员身份，不携带旧身份权限。
     *
     * @param authenticatedAccountId Auth 已验证凭证的账号 ID，不能信任客户端声明
     * @param domain 用户明确选择的域
     * @param tenantId 租户域必填；平台域必须为空
     * @return 经数据库校验的单一身份及状态版本，不代表已获得业务操作
     * @throws BizException 身份不存在或不可用返回 IdentityInvalid，数据库失败返回 AuthorizationUnavailable
     */
    public ActiveIdentity selectAuthenticated(String authenticatedAccountId,
            com.ingot.framework.commons.model.iam.AuthorizationDomain domain, String tenantId) {
        try {
            return identities.selectActive(authenticatedAccountId, domain, tenantId)
                    .orElseThrow(() -> new BizException(IamReasonCode.IDENTITY_INVALID));
        } catch (DataAccessException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
    }

}
