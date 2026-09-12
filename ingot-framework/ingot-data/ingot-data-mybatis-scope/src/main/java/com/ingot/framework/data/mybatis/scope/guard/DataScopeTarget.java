package com.ingot.framework.data.mybatis.scope.guard;

import lombok.Builder;
import lombok.Getter;

/**
 * <p>写归属校验目标：租户、部门与归属用户。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class DataScopeTarget {

    /**
     * 目标租户 ID；空则只校验当前租户上下文。
     */
    private final Long tenantId;

    /**
     * 目标部门 ID。
     */
    private final Long deptId;

    /**
     * 目标归属用户 ID。
     */
    private final Long userId;
}
