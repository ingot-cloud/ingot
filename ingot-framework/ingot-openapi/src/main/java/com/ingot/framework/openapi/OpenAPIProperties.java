package com.ingot.framework.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : SwaggerProperties.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/20.</p>
 * <p>Time         : 09:31.</p>
 */
@Data
@ConfigurationProperties("ingot.openapi")
public class OpenAPIProperties {
    /**
     * 标题
     **/
    private String title = "";

    /**
     * 网关
     */
    private String gateway;

    /**
     * 获取token
     */
    private String tokenUrl;

    /**
     * 作用域
     */
    private String scope;
}
