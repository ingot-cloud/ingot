package com.ingot.framework.security.oauth2.server.resource.access.expression;

import java.util.Set;

/**
 * <p>为已认证 IAM 身份提供本次请求的在线权限，依赖失败必须拒绝，不能退回令牌快照。</p>
 * @author jy
 * @since 1.0.0
 */
public interface TrustedAuthoritySource {
    /** @return 当前可信身份的实时精确操作及服务器系统角色资格 */
    Set<String> currentAuthorities();
}
