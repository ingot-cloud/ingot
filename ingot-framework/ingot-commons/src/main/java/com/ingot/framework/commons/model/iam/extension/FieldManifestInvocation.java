package com.ingot.framework.commons.model.iam.extension;

/**
 * <p>清单 RPC 只使用服务身份，不携带或伪造登录成员身份。</p>
 * @param purpose 固定签名用途
 * @param callerService 已授权调用服务
 * @param resource 固定资源身份
 * @author jy
 * @since 1.0.0
 */
public record FieldManifestInvocation(String purpose, String callerService, ResourceKey resource) {
    /** 签名用途与其他 RPC 隔离。 */
    public static final String PURPOSE = "FIELD_BINDING_MANIFEST";
    /** IAM 清单消费服务。 */
    public static final String IAM_CALLER = "ingot-iam";
}
