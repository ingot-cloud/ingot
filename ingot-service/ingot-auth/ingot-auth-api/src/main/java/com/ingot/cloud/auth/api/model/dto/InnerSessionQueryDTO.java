package com.ingot.cloud.auth.api.model.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>在线会话查询条件，Feign 侧经 {@code @SpringQueryMap} 展开为 query 参数。</p>
 *
 * <p>字段名即线上参数名，Controller 直接以本对象承接绑定，两侧不存在第二份参数名定义。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote {@code clientId} 为空表示不限 Client（当前租户下全部 Client），
 * 该语义仅对按用户维度的查询生效；分页查询要求 {@code clientId} 必填，
 * 因为分页游标建立在「租户 + Client」的在线用户有序集合上。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerSessionQueryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private String clientId;

    /**
     * 按用户过滤，为空表示不限用户。
     */
    private Long userId;

    /**
     * 按登录 IP 过滤，为空表示不限 IP。
     */
    private String ipAddress;

    /**
     * 页码，从 1 开始。
     */
    private Long current;

    /**
     * 每页条数。
     */
    private Long size;
}
