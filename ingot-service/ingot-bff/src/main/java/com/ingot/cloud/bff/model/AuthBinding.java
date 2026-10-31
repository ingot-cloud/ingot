package com.ingot.cloud.bff.model;

import lombok.Data;

/**
 * <p>
 * AuthBinding 的服务端状态，不直接序列化到浏览器。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@lombok.ToString(onlyExplicitlyIncluded = true)
public class AuthBinding {

    /** 随机浏览器绑定。 */
    private String bindingId;

    /** 可信应用。 */
    private String appId;

    /** CSRF 随机秘密。 */
    private String csrfToken;

}
