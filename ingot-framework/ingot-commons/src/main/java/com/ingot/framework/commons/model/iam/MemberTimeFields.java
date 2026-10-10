package com.ingot.framework.commons.model.iam;

/**
 * <p>平台成员详情只读时间字段的稳定逻辑键，不扩充成员资料可写枚举。</p>
 * @author jy
 * @since 1.0.0
 */
public final class MemberTimeFields {
    /** 首次加入时间。 */
    public static final String JOINED_AT = "joinedAt";
    /** 账号最近成功登录时间。 */
    public static final String LAST_LOGIN_AT = "lastLoginAt";
    /** 成员记录更新时间。 */
    public static final String UPDATED_AT = "updatedAt";
    private MemberTimeFields() { }
}
