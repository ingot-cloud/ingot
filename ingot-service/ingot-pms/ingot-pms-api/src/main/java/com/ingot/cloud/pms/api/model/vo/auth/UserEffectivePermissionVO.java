package com.ingot.cloud.pms.api.model.vo.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>当前登录用户的有效具体权限，供前端按钮与路由守卫使用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "当前用户有效权限")
public class UserEffectivePermissionVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前启用的具体权限码。
     */
    @Schema(description = "启用的具体权限码")
    private Set<String> permissions = new LinkedHashSet<>();

    /**
     * 快照版本。
     */
    @Schema(description = "快照版本")
    private Long version;

    /**
     * 源端生成时刻。
     */
    @Schema(description = "生成时刻")
    private Instant generatedAt;

    /**
     * 绝对过期时刻。
     */
    @Schema(description = "过期时刻")
    private Instant expiresAt;
}
