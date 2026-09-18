package com.ingot.cloud.bff.model;

import lombok.Data;

/**
 * 当前 host 的临时浏览器绑定。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class AuthBinding {
    private String bindingId;
    private String appId;
    private String csrfToken;
}
