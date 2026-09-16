package com.ingot.framework.security.core.userdetails;

/**
 * <p>统一认证用户序列化字段名及 IAM 成员上下文字段名。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public interface InUserFieldNames {

    String ID = "id";
    String TENANT_ID = "tenantId";
    String CLIENT_ID = "clientId";
    String TOKEN_AUTH_TYPE = "tokenAuthType";
    String USER_TYPE = "userType";
    String USERNAME = "username";
    String AUTHORITIES = "authorities";
    String DEPT_IDS = "deptIds";
    /** 经验证的 IAM 单一成员身份字段。 */
    String AUTHORIZATION_CONTEXT = "authorizationContext";
    /** IAM 身份管理域字段。 */
    String IDENTITY_DOMAIN = "domain";
    /** IAM 身份全局账号字段。 */
    String IDENTITY_ACCOUNT_ID = "accountId";
    /** IAM 身份成员字段。 */
    String IDENTITY_MEMBER_ID = "memberId";
    String TENANT_DEPT_IDS = "tenantDeptIds";
}
