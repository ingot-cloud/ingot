package com.ingot.framework.authorization;

import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;

/**
 * <p>本地与远程使用同源授权结果；写操作由注册元数据强制最新求值。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AuthorizationClient {

    /**
     * 从当前可信认证恢复身份并求值。
     * @param request 服务器声明的资源操作
     * @return 同源结论
     */
    AuthorizationDecision evaluate(AuthorizationRequest request);

    /** 交互预览，可复用读缓存；不能代替事务最终写门禁。 */
    default AuthorizationDecision preview(AuthorizationRequest request) { return evaluate(request); }

}
