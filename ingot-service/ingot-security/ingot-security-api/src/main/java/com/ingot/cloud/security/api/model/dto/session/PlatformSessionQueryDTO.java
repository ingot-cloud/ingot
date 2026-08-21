package com.ingot.cloud.security.api.model.dto.session;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * <p>安全中心在线会话查询条件，以 query 参数绑定。</p>
 *
 * <p>字段名即线上参数名。{@code clientId} 与 {@code userId} 至少给一个：
 * 分页游标建立在「租户 + Client」的在线用户有序集合上，不限 Client 的全量翻页
 * 在会话存储层没有对应索引；给定 {@code userId} 时可跨 Client 查询，
 * 由安全中心在内存中分页。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote {@code tenantId} 留空表示当前登录管理员所属租户。
 */
@Data
public class PlatformSessionQueryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID，为空取当前租户上下文。
     */
    private Long tenantId;

    /**
     * OAuth2 Client，未指定 {@code userId} 时必填。
     */
    private String clientId;

    /**
     * 按用户过滤，为空表示不限用户。
     */
    private Long userId;

    /**
     * 按登录 IP 过滤，为空表示不限 IP；需与 {@code clientId} 同时给出。
     */
    private String ipAddress;

    /**
     * 页码，从 1 开始，为空按 1 处理。
     */
    private Long current;

    /**
     * 每页条数，为空按 20 处理。
     */
    private Long size;
}
