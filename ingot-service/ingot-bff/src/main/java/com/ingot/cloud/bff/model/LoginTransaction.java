package com.ingot.cloud.bff.model;

import lombok.Data;
import java.util.List;
import com.ingot.framework.commons.model.bff.BffLoginStage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import lombok.ToString;

/**
 * <p>
 * LoginTransaction 的服务端状态，不直接序列化到浏览器。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class LoginTransaction {

    /** 随机事务。 */
    private String transactionId;

    /** 可信应用。 */
    private String appId;

    /** 固定授权域。 */
    private AuthorizationDomain domain;

    /** 创建时的唯一浏览器绑定。 */
    private String bindingId;

    /** 服务端状态。 */
    private BffLoginStage stage;

    /** CAS 栅栏版本；每次写入递增。 */
    private long version;

    /** UTC 秒数的硬截止。 */
    private long expiresAt;

    /** Auth 真实认证窗口截止秒数。 */
    private long authenticationExpiresAt;

    /** 当前执行租约截止秒数，不接管重做授权。 */
    private long leaseUntil;

    /** PKCE verifier，仅服务端。 */
    private String codeVerifier;

    /** OAuth state，仅服务端。 */
    private String state;

    /** Auth SecurityContext Cookie，仅服务端。 */
    private String authCookie;

    /** 待选择的原始候选。 */
    private List<TenantMainDTO> allows;

    /** 已提交的正式会话引用，仅服务端。 */
    private String resultSessionId;

    /** 本次结果交付/恢复的截止秒数。 */
    private long recoveryExpiresAt;

}
