package com.ingot.framework.security.config.annotation.web;


import com.ingot.framework.core.constants.SecurityConstants;

/**
 * <p>Description  : PermitModel.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/12.</p>
 * <p>Time         : 2:04 PM.</p>
 */
public enum PermitModel {
    /**
     * 公开接口，不校验任何逻辑
     */
    PUBLIC,
    /**
     * 内部接口，该模式不校验任何权限，但是必须为内部请求（微服务之间的请求，
     * 内部请求会增加请求头 {@link SecurityConstants#HEADER_FROM}，其值
     * 必须为 {@link SecurityConstants#HEADER_FROM_INSIDE_VALUE}）
     */
    INNER,
}
