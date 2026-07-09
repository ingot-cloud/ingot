package com.ingot.cloud.bff.model.dto;

import lombok.Data;

/**
 * <p>BFF 登录请求参数，前端只需提交业务字段，不含任何 OAuth2 参数</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class BffLoginDTO {
    private String username;
    private String password;
}
