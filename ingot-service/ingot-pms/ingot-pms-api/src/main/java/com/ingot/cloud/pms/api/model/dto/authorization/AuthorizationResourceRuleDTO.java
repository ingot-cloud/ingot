package com.ingot.cloud.pms.api.model.dto.authorization;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.Data;

/**
 * <p>授权快照中某一 {@code (resource, permission)} 的合并数据范围。</p>
 *
 * <p>{@link #scopeType} 为 {@link DataScopeTypeEnum#ALL} 时消去该资源行过滤；
 * 否则执行层按 {@link #self} 与 {@link #deptIds} 做 {@code self OR dept} 并集。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class AuthorizationResourceRuleDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 资源编码，与 {@code platform_resource.code} 一致。
     */
    private String resourceCode;

    /**
     * 具体功能权限码。
     */
    private String permissionCode;

    /**
     * 合并后的范围类型；{@link DataScopeTypeEnum#ALL} 表示该资源跳过行过滤。
     */
    private DataScopeTypeEnum scopeType;

    /**
     * 可访问部门 ID 并集；ALL 时为空。
     */
    private List<Long> deptIds = new ArrayList<>();

    /**
     * 是否包含本人范围。
     */
    private Boolean self = Boolean.FALSE;
}
