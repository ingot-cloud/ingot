package com.ingot.framework.commons.model.iam;

/**
 * <p>当前认证身份的最小改密状态，不披露业务资料、菜单或角色。</p>
 *
 * @param context 当前服务器验证的授权身份
 * @param mustChangePassword 是否必须先修改密码
 * @author jy
 * @since 1.0.0
 */
public record PasswordChangeState(AuthorizationContext context, boolean mustChangePassword) {
}
