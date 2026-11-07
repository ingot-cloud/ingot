package com.ingot.cloud.iam.domain.identity;

/**
 * <p>表示社交绑定命中的全局账号，不携带凭证、成员身份或授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param accountId 已绑定的全局账号ID
 */
public record AccountSocialPrincipal(Long accountId) {
}
