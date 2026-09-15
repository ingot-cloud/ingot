package com.ingot.cloud.iam.identity;

import com.ingot.framework.commons.model.iam.AuthorizationContext;

/**
 * <p>保存同一数据库查询确认的有效身份与状态版本，不包含凭证或业务权限。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param context 已验证账号归属及有效状态的身份
 * @param accountVersion 全局账号版本
 * @param memberVersion 当前域成员版本
 * @param tenantVersion 租户版本；平台身份为空
 */
public record ActiveIdentity(AuthorizationContext context, String accountVersion,
                             String memberVersion, String tenantVersion) {
}
