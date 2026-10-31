package com.ingot.cloud.bff.model;

import lombok.Data;

/**
 * <p>
 * BffCleanup 的服务端状态，不直接序列化到浏览器。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@lombok.ToString(onlyExplicitlyIncluded = true)
public class BffCleanup {

    /** 需要确认或撤销的 BFF 会话。 */
    private String sessionId;

    /** 任务应用。 */
    private String appId;

    /** 任务绑定。 */
    private String bindingId;

    /** 原始事务。 */
    private String transactionId;

    /** 只撤销本任务持有的 Auth sid。 */
    private String sid;

    /** 撤销 Auth Context 的内部 Cookie。 */
    private String authCookie;

    /** true 表示先验证交付确认，false 表示必须撤销。 */
    private boolean pendingDelivery;

}
