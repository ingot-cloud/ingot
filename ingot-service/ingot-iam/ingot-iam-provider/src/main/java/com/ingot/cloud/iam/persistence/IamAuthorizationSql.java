package com.ingot.cloud.iam.persistence;

/**
 * <p>平台授权变更共用的首个写锁，相关事务先锁应用再锁业务行。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IamAuthorizationSql {
    /** 平台授权治理应用的写锁语句。 */
    public static final String PLATFORM_WRITE_LOCK =
            "SELECT id FROM iam_application WHERE code='iam-platform' AND domain='PLATFORM' FOR UPDATE";
    private IamAuthorizationSql() { }
}
